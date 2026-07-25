package com.costumi.app.ui.cliente.detalle

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.UiState
import com.costumi.app.core.comoPrecio
import com.costumi.app.ui.alBuscar
import com.costumi.app.databinding.FragmentDetalleDisfrazBinding
import com.costumi.app.databinding.ItemPiezaDisfrazBinding
import com.costumi.app.databinding.ItemPreviewPiezaBinding
import com.costumi.app.databinding.SheetElegirPiezaBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.app.ui.cliente.carrito.CarritoFragment
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.common.SelectorDeCantidad
import com.costumi.app.ui.common.SelectorDePeriodo
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.AgregarItemRequest
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.FacetaDto
import com.costumi.apiclient.models.SucursalVitrinaResponse
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Detalle de un disfraz: arma las piezas (ruleta), elige rentar/comprar y va al pago. */
@AndroidEntryPoint
class DetalleDisfrazFragment : Fragment(R.layout.fragment_detalle_disfraz) {

    private val vm: DetalleDisfrazViewModel by viewModels()
    private var _binding: FragmentDetalleDisfrazBinding? = null
    private val binding get() = _binding!!

    private var modoRenta = true
    /** Delegan en el ViewModel: la vista se recrea al volver atras y ahi se perdian. */
    private var retiro: LocalDate?
        get() = vm.fechaRetiro
        set(valor) { vm.fechaRetiro = valor }
    private var devolucion: LocalDate?
        get() = vm.fechaDevolucion
        set(valor) { vm.fechaDevolucion = valor }
    private var periodo: SelectorDePeriodo? = null
    private var selectorCantidad: SelectorDeCantidad? = null
    /** Sobreviven a recrear la vista (volver del carrito). */
    private var cantidad: Int = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDetalleDisfrazBinding.bind(view)
        binding.toolbar.title = vm.nombre
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.nombre.text = vm.nombre

        binding.grupoModo.check(R.id.botonRentar)
        aplicarModo()
        binding.grupoModo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            modoRenta = checkedId == R.id.botonRentar
            aplicarModo()
        }

        periodo = SelectorDePeriodo(this, binding.periodo, "Fechas de renta") { desde, hasta ->
            retiro = desde
            devolucion = hasta
            // El total depende de los dias elegidos: sin esto la barra seguiria mostrando $0.
            recalcular()
        }
        periodo?.fijar(retiro, devolucion)
        selectorCantidad = SelectorDeCantidad(binding.cantidad, inicial = cantidad) {
            cantidad = it
            recalcular()
        }
        binding.botonFavorito.setOnClickListener { vm.alternarFavorito() }
        observar(vm.esFavorito) { esFav ->
            binding.botonFavorito.setImageResource(
                if (esFav) R.drawable.ic_corazon_lleno else R.drawable.ic_corazon,
            )
            val attr = if (esFav) {
                androidx.appcompat.R.attr.colorPrimary
            } else {
                com.google.android.material.R.attr.colorOnSurfaceVariant
            }
            binding.botonFavorito.setColorFilter(
                com.google.android.material.color.MaterialColors.getColor(binding.botonFavorito, attr),
            )
        }
        binding.botonContinuar.setOnClickListener {
            // El boton hace LO QUE DICE: si nombra una pieza que falta, la abre; si no, agrega.
            val faltante = piezaFaltante()
            if (faltante != null) {
                abrirEleccionDePieza(faltante)
            } else {
                val tipo = if (modoRenta) AgregarItemRequest.Tipo.RENTA else AgregarItemRequest.Tipo.VENTA
                vm.agregarAlCarrito(tipo, cantidad, retiro, devolucion)
            }
        }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "Este disfraz no esta disponible.") { ui -> pintar(ui) }
        }
        observar(vm.cargando) { cargando ->
            binding.progreso.isVisible = cargando
            binding.botonContinuar.isEnabled = !cargando
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoDisfraz.Agregado -> irAlCarrito(evento)
                is EventoDisfraz.Error -> mostrarMensaje(evento.mensaje)
            }
        }
        observar(vm.sucursales) { lista ->
            binding.seccionRetiro.isVisible = lista.isNotEmpty()
            binding.botonSucursal.setOnClickListener(
                if (lista.size > 1) View.OnClickListener { elegirSucursal(lista) } else null,
            )
        }
        observar(vm.sucursalSeleccionada) { sucursal ->
            binding.botonSucursal.text = sucursal?.nombre ?: "Elegir sucursal"
        }
    }

    private fun pintar(ui: DetalleUi) {
        binding.foto.cargarFoto(ui.disfraz.fotoUrl)
        binding.disponibilidad.text = if (ui.disponible) "Disponible" else "No disponible ahora"

        // El dueño decide para qué está disponible: el cliente solo ve/puede hacer lo habilitado.
        val permiteRenta = ui.disfraz.tipo != DisfrazResponse.Tipo.VENTA
        val permiteVenta = ui.disfraz.tipo != DisfrazResponse.Tipo.RENTA
        binding.botonRentar.isVisible = permiteRenta
        binding.botonComprar.isVisible = permiteVenta
        // Si solo hay una opción, no tiene sentido mostrar el selector.
        binding.grupoModo.isVisible = permiteRenta && permiteVenta
        if (modoRenta && !permiteRenta) modoRenta = false
        if (!modoRenta && !permiteVenta) modoRenta = true
        binding.grupoModo.check(if (modoRenta) R.id.botonRentar else R.id.botonComprar)
        aplicarModo()

        val renta = (ui.disfraz.precioRentaGeneral ?: ui.disfraz.precioRentaSugerido).comoPrecio()
            ?.takeIf { permiteRenta }?.let { "Renta $it" }
        val venta = ui.disfraz.precioVentaSugerido.comoPrecio()
            ?.takeIf { permiteVenta }?.let { "Venta $it" }
        binding.precios.text = listOfNotNull(renta, venta).joinToString("   ·   ")
        construirSlots(ui.slots)
        actualizarTotal(ui)
    }

    /**
     * Arma la lista de piezas: **una fila por pieza**, no un carrusel por pieza.
     *
     * Antes cada slot mostraba todas sus opciones en linea, asi que un disfraz de 8 piezas era una
     * pantalla interminable y no se veia de un vistazo que faltaba elegir. Ahora cada fila muestra lo
     * YA elegido, y las opciones se eligen en una hoja dedicada (ver [abrirEleccionDePieza]).
     */
    private fun construirSlots(slots: List<SlotUi>) {
        val contenedor = binding.contenedorSlots
        contenedor.removeAllViews()
        for (slot in slots) {
            val fila = ItemPiezaDisfrazBinding.inflate(layoutInflater, contenedor, false)
            val elegida = slot.opciones.firstOrNull { it.prendaId == vm.seleccionDe(slot.orden) }

            fila.nombrePieza.text = slot.nombre
            fila.miniatura.cargarFoto(elegida?.fotoUrl)

            when {
                elegida != null -> {
                    fila.detallePieza.text = elegida.nombre.orEmpty()
                    // Talla/color de lo elegido, igual que en la grilla: sin esto, dos variantes de nombre
                    // casi igual (M vs L) se ven idénticas en la fila.
                    val etiquetas = elegida.etiquetas.orEmpty()
                        .mapNotNull { it.valorNombre?.takeIf { valor -> valor.isNotBlank() } }
                        .joinToString("  ·  ")
                    fila.etiquetasPieza.text = etiquetas
                    fila.etiquetasPieza.isVisible = etiquetas.isNotEmpty()
                    fila.marcaElegida.isVisible = true
                    fila.accionElegir.isVisible = false
                }
                slot.opciones.isEmpty() -> {
                    // Sin opciones con stock: se dice por que, en vez de ofrecer una pieza vacia.
                    fila.detallePieza.text = "Sin opciones disponibles ahora"
                    fila.marcaElegida.isVisible = false
                    fila.accionElegir.isVisible = false
                }
                else -> {
                    val n = slot.opciones.size
                    fila.detallePieza.text = if (n == 1) "1 opcion" else "$n opciones"
                    fila.marcaElegida.isVisible = false
                    fila.accionElegir.isVisible = true
                }
            }

            if (slot.opciones.isNotEmpty()) {
                fila.tarjetaPieza.setOnClickListener { abrirEleccionDePieza(slot) }
            }
            contenedor.addView(fila.root)
        }
        pintarPreview(slots)
        actualizarProgreso(slots)
    }

    /**
     * Vista previa gráfica del disfraz armado: una fila con la foto de cada pieza. Las ya elegidas se ven
     * a color; las pendientes, atenuadas, para que el cliente vea el disfraz tomando forma antes de
     * agregarlo. Aparece en cuanto hay al menos una pieza elegida.
     */
    private fun pintarPreview(slots: List<SlotUi>) {
        val cont = binding.contenedorPreview
        cont.removeAllViews()
        val elegibles = slots.filter { it.opciones.isNotEmpty() }
        val hayElegidas = elegibles.any { vm.seleccionDe(it.orden) != null }
        binding.previewScroll.isVisible = hayElegidas
        if (!hayElegidas) return
        for (slot in elegibles) {
            val elegida = slot.opciones.firstOrNull { it.prendaId == vm.seleccionDe(slot.orden) }
            val tile = ItemPreviewPiezaBinding.inflate(layoutInflater, cont, false)
            tile.nombre.text = slot.nombre
            if (elegida != null) {
                tile.foto.cargarFoto(elegida.fotoUrl)
                tile.root.alpha = 1f
            } else {
                tile.foto.setImageResource(R.drawable.ic_imagen_placeholder)
                tile.root.alpha = 0.35f
            }
            cont.addView(tile.root)
        }
    }

    /**
     * Precio real de lo que se esta armando, en la barra de accion.
     *
     * Antes el boton estaba solo: el cliente no sabia cuanto iba a pagar hasta llegar al carrito, y en
     * una renta el numero cambia mucho segun los dias (precio x dias x cantidad).
     */
    private fun actualizarTotal(ui: DetalleUi) {
        val unitario = if (modoRenta) {
            ui.disfraz.precioRentaGeneral ?: ui.disfraz.precioRentaSugerido
        } else {
            ui.disfraz.precioVentaGeneral ?: ui.disfraz.precioVentaSugerido
        }
        if (unitario == null) {
            binding.textoTotal.text = ""
            binding.textoTotalDetalle.text = ""
            return
        }
        val dias = diasDeRenta()
        val total = unitario
            .multiply(java.math.BigDecimal(cantidad))
            .multiply(java.math.BigDecimal(if (modoRenta) dias else 1))

        binding.textoTotal.text = total.comoPrecio().orEmpty()
        binding.textoTotalDetalle.text = when {
            modoRenta && dias > 0 -> "${unitario.comoPrecio()}/dia × $dias dias" +
                (if (cantidad > 1) " × $cantidad" else "")
            modoRenta -> "Elegi las fechas para ver el total"
            cantidad > 1 -> "${unitario.comoPrecio()} c/u × $cantidad"
            else -> "Precio de venta"
        }
    }

    /** Dias entre las fechas elegidas; 0 si todavia no se eligieron. Minimo 1 dia, como el backend. */
    private fun diasDeRenta(): Int {
        val r = retiro
        val d = devolucion
        if (r == null || d == null) return 0
        return java.time.temporal.ChronoUnit.DAYS.between(r, d).toInt().coerceAtLeast(1)
    }

    /** La primera pieza obligatoria sin elegir, o null si el disfraz esta completo. */
    private fun piezaFaltante(): SlotUi? =
        (vm.estado.value as? UiState.Success<DetalleUi>)?.data?.slots
            ?.firstOrNull { !it.opcional && it.opciones.isNotEmpty() && vm.seleccionDe(it.orden) == null }

    /**
     * Hoja de seleccion de una pieza: grilla con foto, precio y stock, con buscador.
     *
     * Se abre casi a pantalla completa porque es el momento en que el cliente **compara**; hacerlo en
     * una tira horizontal dentro del detalle obligaba a desplazarse a ciegas.
     */
    private fun abrirEleccionDePieza(slot: SlotUi) {
        val sheetBinding = SheetElegirPiezaBinding.inflate(layoutInflater)
        val hoja = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        hoja.setContentView(sheetBinding.root)
        // Sheet alto, expandido y NO arrastrable: deslizar solo scrollea la grilla; no se cierra al deslizar.
        (sheetBinding.root.parent as? View)?.let { contenedorSheet ->
            com.google.android.material.bottomsheet.BottomSheetBehavior.from(contenedorSheet).apply {
                state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
                isDraggable = false
            }
            contenedorSheet.layoutParams = contenedorSheet.layoutParams.apply {
                height = (resources.displayMetrics.heightPixels * 0.92).toInt()
            }
        }
        sheetBinding.titulo.text = slot.nombre

        // Valores de etiqueta elegidos como filtro (por id). Vacío = sin filtro. El estado del conjunto se
        // guarda en estas variables porque cada cambio de filtro re-consulta la ruleta (conteos dinámicos).
        val seleccionadas = linkedSetOf<UUID>()
        var opcionesActuales = slot.opciones
        var facetasActuales = slot.facetas
        var textoBusqueda = ""

        val adapter = OpcionGrillaAdapter { opcion ->
            vm.seleccionar(slot.orden, opcion.prendaId)
            // Repinta la lista con la eleccion nueva y recalcula progreso y accion principal.
            (vm.estado.value as? UiState.Success<DetalleUi>)?.data?.slots?.let { construirSlots(it) }
            hoja.dismiss()
        }
        adapter.seleccionadaId = vm.seleccionDe(slot.orden)
        sheetBinding.listaOpciones.adapter = adapter

        // Re-consulta la ruleta con el filtro actual y repinta. `render` se define abajo y se pasa por nombre.
        fun refiltrar(render: () -> Unit) {
            viewLifecycleOwner.lifecycleScope.launch {
                vm.opcionesFiltradas(slot.orden, seleccionadas.toList())?.let {
                    opcionesActuales = it.opciones.orEmpty()
                    facetasActuales = it.facetas.orEmpty()
                }
                render()
            }
        }

        fun render() {
            val visibles = if (textoBusqueda.isBlank()) {
                opcionesActuales
            } else {
                opcionesActuales.filter { it.nombre.orEmpty().contains(textoBusqueda, ignoreCase = true) }
            }
            adapter.submitList(visibles)

            val total = opcionesActuales.size
            sheetBinding.subtitulo.text =
                (if (total == 1) "1 opcion" else "$total opciones") +
                    if (slot.opcional) " · pieza opcional" else " · elegi una"

            val vacio = visibles.isEmpty()
            sheetBinding.listaOpciones.isVisible = !vacio
            sheetBinding.textoVacio.isVisible = vacio
            // El vacío por filtro no es lo mismo que el vacío por búsqueda: cada uno se resuelve distinto.
            sheetBinding.textoVacio.text =
                if (opcionesActuales.isEmpty() && seleccionadas.isNotEmpty()) {
                    "Ninguna combinacion disponible. Quita un filtro."
                } else {
                    "Ninguna opcion coincide con lo que buscaste."
                }
            sheetBinding.botonLimpiarFiltros.isVisible = seleccionadas.isNotEmpty()

            pintarFiltros(sheetBinding.contenedorFiltros, facetasActuales, seleccionadas) { valorId ->
                if (!seleccionadas.add(valorId)) seleccionadas.remove(valorId)
                refiltrar(::render)
            }
        }

        sheetBinding.botonLimpiarFiltros.setOnClickListener {
            seleccionadas.clear()
            refiltrar(::render)
        }
        // Buscar entre las opciones: con muchas prendas, escribir es mas rapido que desplazar.
        sheetBinding.editBuscar.alBuscar { texto ->
            textoBusqueda = texto
            render()
        }

        render()
        hoja.show()
    }

    /**
     * Reconstruye las filas de filtros (una por dimensión) desde las facetas: un rótulo por dimensión y un
     * chip por valor con su conteo. Togglear un chip llama a [alToggle] con el id del valor. Se oculta si el
     * slot no tiene dimensiones filtrables (etiquetas seleccionables por el cliente).
     */
    private fun pintarFiltros(
        contenedor: LinearLayout,
        facetas: List<FacetaDto>,
        seleccionadas: Set<UUID>,
        alToggle: (UUID) -> Unit,
    ) {
        contenedor.removeAllViews()
        contenedor.isVisible = facetas.isNotEmpty()
        val margen = (6 * resources.displayMetrics.density).toInt()
        for (faceta in facetas) {
            val rotulo = TextView(contenedor.context).apply {
                text = faceta.tipoNombre.orEmpty()
                textSize = 12f
                setPadding(0, margen, 0, margen / 2)
            }
            contenedor.addView(rotulo)
            val grupo = ChipGroup(contenedor.context).apply { isSingleLine = false }
            for (valor in faceta.valores.orEmpty()) {
                val id = valor.valorEtiquetaId ?: continue
                val chip = layoutInflater.inflate(R.layout.chip_filtro, grupo, false) as Chip
                chip.text = "${valor.valorNombre.orEmpty()}  ·  ${valor.cantidad ?: 0}"
                chip.isChecked = id in seleccionadas
                chip.setOnClickListener { alToggle(id) }
                grupo.addView(chip)
            }
            contenedor.addView(grupo)
        }
    }

    /**
     * Progreso y accion principal. El boton **nombra la pieza que falta** ("Elegir botas") en vez de
     * un "Continuar" generico: el cliente no tiene que deducir cual es el proximo paso.
     */
    private fun actualizarProgreso(slots: List<SlotUi>) {
        // Las piezas opcionales no cuentan para el progreso: exigirlas dejaria el disfraz "incompleto"
        // para siempre aunque el cliente no las quiera.
        val obligatorias = slots.filter { !it.opcional && it.opciones.isNotEmpty() }
        val elegidas = obligatorias.count { vm.seleccionDe(it.orden) != null }
        val total = obligatorias.size

        binding.textoProgreso.text = if (total == 0) "" else "$elegidas de $total piezas"
        binding.barraProgreso.isVisible = total > 0
        binding.barraProgreso.max = if (total == 0) 1 else total
        binding.barraProgreso.setProgressCompat(elegidas, true)

        val faltante = obligatorias.firstOrNull { vm.seleccionDe(it.orden) == null }
        binding.botonContinuar.text = when {
            faltante != null -> "Elegir ${faltante.nombre.lowercase()}"
            else -> "Agregar al carrito"
        }
    }

    private fun aplicarModo() {
        binding.seccionFechas.isVisible = modoRenta
        recalcular()
    }

    /** Repinta el total cuando cambia algo que lo afecta (fechas, cantidad, renta/venta). */
    private fun recalcular() {
        (vm.estado.value as? UiState.Success<DetalleUi>)?.data?.let { actualizarTotal(it) }
    }

    private fun elegirSucursal(lista: List<SucursalVitrinaResponse>) {
        val nombres = lista.map { s -> listOfNotNull(s.nombre, s.direccion).joinToString("  -  ") }.toTypedArray()
        val actual = lista.indexOfFirst { it.id == vm.sucursalSeleccionada.value?.id }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Elige donde retirar")
            .setSingleChoiceItems(nombres, actual) { dialog, which ->
                lista[which].id?.toString()?.let { vm.seleccionarSucursal(it) }
                dialog.dismiss()
            }
            .show()
    }


    private fun irAlCarrito(evento: EventoDisfraz.Agregado) {
        mostrarMensaje("Agregado al carrito")
        findNavController().navigate(
            R.id.carritoFragment,
            bundleOf(
                CarritoFragment.ARG_EMPRESA_ID to evento.empresaId,
                CarritoFragment.ARG_SUCURSAL_ID to evento.sucursalId,
                CarritoFragment.ARG_TIPO to evento.tipo.value,
            ),
        )
    }

    override fun onDestroyView() {
        binding.contenedorSlots.removeAllViews()
        binding.contenedorPreview.removeAllViews()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_EMPRESA_ID = "empresaId"
        const val ARG_DISFRAZ_ID = "disfrazId"
        const val ARG_NOMBRE = "nombre"
    }
}
