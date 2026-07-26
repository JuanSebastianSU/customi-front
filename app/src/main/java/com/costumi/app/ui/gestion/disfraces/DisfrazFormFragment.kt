package com.costumi.app.ui.gestion.disfraces

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.FragmentDisfrazFormBinding
import com.costumi.app.databinding.ItemSlotFormBinding
import com.costumi.app.databinding.SheetElegirPiezaBinding
import com.costumi.app.data.repo.TipoConValores
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.cargarFoto
import com.costumi.app.ui.common.ListaBuscable
import com.costumi.app.ui.common.OpcionBuscable
import com.costumi.app.ui.extensionDeImagen
import com.costumi.app.ui.leerBytesDeImagen
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.app.ui.soloImagenes
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.costumi.apiclient.models.CategoriaResponse
import com.costumi.apiclient.models.CategoriaDeDisfrazResponse
import com.costumi.apiclient.models.CrearDisfrazRequest
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.PrendaDeCatalogoResponse
import com.costumi.apiclient.models.PrendaResponse
import com.costumi.apiclient.models.SlotDto
import java.math.BigDecimal
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

/** Alta/edición de disfraz: nombre + precio + piezas (FIJA→prenda, o PERSONALIZABLE→pool de categoría). */
@AndroidEntryPoint
class DisfrazFormFragment : Fragment(R.layout.fragment_disfraz_form) {

    private val vm: DisfrazFormViewModel by viewModels()
    private var _binding: FragmentDisfrazFormBinding? = null
    private val binding get() = _binding!!

    private class FilaSlot(
        val binding: ItemSlotFormBinding,
        var tipo: SlotDto.EjePrenda,
        var prenda: PrendaResponse?,
        var categoria: CategoriaResponse?,
        // Parte personalizable: los elementos (prendas concretas del inventario) que el dueño eligió como
        // opciones de esta parte. La categoría, si se elige, solo filtra el picker.
        val opciones: MutableList<PrendaDeCatalogoResponse> = mutableListOf(),
        // Filtro por valores de etiqueta del picker (tipoId -> valorIds). Acota qué prendas se ofrecen.
        val etiquetas: MutableMap<UUID, MutableSet<UUID>> = mutableMapOf(),
    )

    private val filas = mutableListOf<FilaSlot>()
    private var prendas: List<PrendaResponse> = emptyList()
    private var categorias: List<CategoriaResponse> = emptyList()
    private var categoriasDisfraz: List<CategoriaDeDisfrazResponse> = emptyList()
    private var etiquetasDisponibles: List<TipoConValores> = emptyList()
    private var categoriaDisfraz: CategoriaDeDisfrazResponse? = null
    private var prefilled = false

    /** Para qué está disponible el disfraz: lo decide el dueño y acota lo que el cliente puede hacer. */
    private val tiposDisponibilidad = listOf(
        AUTOMATICO to null,
        "Renta y venta" to CrearDisfrazRequest.Tipo.AMBOS,
        "Solo renta" to CrearDisfrazRequest.Tipo.RENTA,
        "Solo venta" to CrearDisfrazRequest.Tipo.VENTA,
    )
    /**
     * null = **automatico**: el backend deriva el tipo de las piezas. Antes venia AMBOS marcado, que es la
     * opcion mas exigente (obliga a que cada pieza sirva para renta y para venta), asi que el dueño recibia
     * un error por una decision que nunca tomo.
     */
    private var tipoDisfraz: CrearDisfrazRequest.Tipo? = null

    private var fotoBytes: ByteArray? = null
    private var fotoMime: String? = null
    private var fotoNombre: String? = null

    private val seleccionarFoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { elegirFoto(it) }
    }

    private val tipos = listOf(
        "Prenda fija" to SlotDto.EjePrenda.FIJA,
        "Personalizable (el cliente elige)" to SlotDto.EjePrenda.PERSONALIZABLE,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDisfrazFormBinding.bind(view)
        binding.toolbar.title = if (vm.esEdicion) "Editar disfraz" else "Nuevo disfraz"
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.botonAgregarSlot.setOnClickListener { agregarSlot() }
        binding.botonGuardar.setOnClickListener { guardar() }

        // Tipo del disfraz: solo se pide el precio de la operación habilitada.
        binding.dropTipo.setSimpleItems(tiposDisponibilidad.map { it.first }.toTypedArray())
        binding.dropTipo.setOnItemClickListener { _, _, pos, _ ->
            tipoDisfraz = tiposDisponibilidad[pos].second
            aplicarTipoDisfraz()
        }
        aplicarTipoDisfraz()

        // La foto del disfraz se sube en edición (el endpoint es por-id, como en prendas).
        // La foto se puede agregar tanto al crear como al editar (se sube al guardar).
        binding.botonFoto.isVisible = true
        binding.botonFoto.text = "Agregar foto"
        binding.botonFoto.setOnClickListener { seleccionarFoto.launch(soloImagenes) }

        observar(vm.datos) { estado ->
            binding.stateView.mostrar(estado, vacio = "Crea prendas antes de armar un disfraz.") { datos ->
                prendas = datos.prendas
                categorias = datos.categorias
                categoriasDisfraz = datos.categoriasDisfraz
                etiquetasDisponibles = datos.etiquetas
                filas.forEach { refrescarOpciones(it) }
                configurarCategoriaDisfraz()
                if (!prefilled) {
                    prefilled = true
                    if (datos.disfraz != null) prefill(datos.disfraz) else agregarSlot()
                }
            }
        }
        observar(vm.guardando) { guardando ->
            binding.botonGuardar.isEnabled = !guardando
            binding.botonGuardar.text = if (guardando) "Guardando..." else "Guardar disfraz"
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoDisfrazForm.Guardado -> {
                    evento.avisoFoto?.let {
                        android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_LONG).show()
                    }
                    setFragmentResult(RESULT_GUARDADO, Bundle.EMPTY)
                    findNavController().popBackStack()
                }
                is EventoDisfrazForm.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    /** Refleja el tipo elegido: muestra solo el/los precio(s) que aplican y sincroniza el selector. */
    private fun aplicarTipoDisfraz() {
        binding.dropTipo.setText(
            tiposDisponibilidad.firstOrNull { it.second == tipoDisfraz }?.first ?: tiposDisponibilidad.first().first,
            false,
        )
        // En automatico todavia no se sabe que aplicara: se ofrecen los dos precios y se manda el que se llene.
        binding.tilPrecioRenta.isVisible = tipoDisfraz != CrearDisfrazRequest.Tipo.VENTA
        binding.tilPrecioVenta.isVisible = tipoDisfraz != CrearDisfrazRequest.Tipo.RENTA
    }

    /** Puebla el selector con las categorías de DISFRAZ ("Sin categoria" + las categorías) y refleja la elegida. */
    private fun configurarCategoriaDisfraz() {
        val items = (listOf(SIN_CATEGORIA) + categoriasDisfraz.map { it.nombre.orEmpty() }).toTypedArray()
        binding.dropCategoriaDisfraz.setSimpleItems(items)
        binding.dropCategoriaDisfraz.setOnItemClickListener { _, _, pos, _ ->
            categoriaDisfraz = if (pos == 0) null else categoriasDisfraz.getOrNull(pos - 1)
        }
        binding.dropCategoriaDisfraz.setText(categoriaDisfraz?.nombre ?: SIN_CATEGORIA, false)
    }

    private fun prefill(disfraz: DisfrazResponse) {
        binding.editNombre.setText(disfraz.nombre.orEmpty())
        disfraz.precioRentaGeneral?.let { binding.editPrecio.setText(it.toPlainString()) }
        disfraz.precioVentaGeneral?.let { binding.editPrecioVenta.setText(it.toPlainString()) }
        // Un disfraz ya guardado siempre tiene un tipo concreto: al editar se muestra ese, no "automatico".
        tipoDisfraz = when (disfraz.tipo) {
            DisfrazResponse.Tipo.RENTA -> CrearDisfrazRequest.Tipo.RENTA
            DisfrazResponse.Tipo.VENTA -> CrearDisfrazRequest.Tipo.VENTA
            else -> CrearDisfrazRequest.Tipo.AMBOS
        }
        aplicarTipoDisfraz()
        categoriasDisfraz.firstOrNull { it.id == disfraz.categoriaId }?.let {
            categoriaDisfraz = it
            binding.dropCategoriaDisfraz.setText(it.nombre.orEmpty(), false)
        }
        if (!disfraz.fotoUrl.isNullOrBlank()) {
            binding.foto.isVisible = true
            binding.foto.cargarFoto(disfraz.fotoUrl)
            binding.botonFoto.text = "Cambiar foto"
        }
        val renta = disfraz.precioRentaSugerido.comoPrecio()
        val venta = disfraz.precioVentaSugerido.comoPrecio()
        if (renta != null || venta != null) {
            binding.sugeridos.isVisible = true
            binding.sugeridos.text = "Sugerido (suma de piezas): " +
                listOfNotNull(renta?.let { "renta $it" }, venta?.let { "venta $it" }).joinToString(" · ")
        }
        disfraz.slots.orEmpty().sortedBy { it.orden ?: 0 }.forEach { slot ->
            val fila = agregarSlot()
            fila.binding.editSlotNombre.setText(slot.nombre.orEmpty())
            fila.binding.switchOpcional.isChecked = slot.opcional == true
            val tipo = slot.ejePrenda ?: SlotDto.EjePrenda.FIJA
            aplicarTipo(fila, tipo)
            tipos.firstOrNull { it.second == tipo }?.let { fila.binding.dropTipoPieza.setText(it.first, false) }
            if (tipo == SlotDto.EjePrenda.FIJA) {
                prendas.firstOrNull { it.id == slot.prendaFijaId }?.let {
                    fila.prenda = it
                    pintarPrendaFija(fila)
                }
            } else {
                // Compat: si el slot viejo tenía pool, se muestra su categoría como filtro del picker.
                categorias.firstOrNull { it.id == slot.pool?.categoriaId }?.let {
                    fila.categoria = it
                    fila.binding.dropCategoria.setText(it.nombre.orEmpty(), false)
                }
                val ids = slot.prendasOpcion.orEmpty().toSet()
                if (ids.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val catalogo = vm.catalogo(null)
                        fila.opciones.clear()
                        catalogo.filter { it.id in ids }.forEach { fila.opciones.add(it) }
                        actualizarEtiquetaOpciones(fila)
                        recomputarSugerido()
                    }
                }
            }
        }
        recomputarSugerido()
    }

    private fun agregarSlot(): FilaSlot {
        val slotBinding = ItemSlotFormBinding.inflate(layoutInflater, binding.contenedorSlots, false)
        val fila = FilaSlot(slotBinding, SlotDto.EjePrenda.FIJA, null, null)

        slotBinding.dropTipoPieza.setSimpleItems(tipos.map { it.first }.toTypedArray())
        slotBinding.dropTipoPieza.setText(tipos.first().first, false)
        slotBinding.dropTipoPieza.setOnItemClickListener { _, _, pos, _ ->
            aplicarTipo(fila, tipos[pos].second)
        }
        slotBinding.tarjetaElegirPrenda.setOnClickListener { abrirSelectorCatalogo(fila, multiple = false) }
        slotBinding.botonEtiquetas.text = "Elegir elementos"
        slotBinding.botonEtiquetas.setOnClickListener { abrirSelectorCatalogo(fila, multiple = true) }
        slotBinding.botonEliminarSlot.setOnClickListener {
            binding.contenedorSlots.removeView(slotBinding.root)
            filas.remove(fila)
            renumerar()
        }
        refrescarOpciones(fila)
        aplicarTipo(fila, SlotDto.EjePrenda.FIJA)
        filas.add(fila)
        binding.contenedorSlots.addView(slotBinding.root)
        renumerar()
        return fila
    }

    /** Actualiza las opciones de los selectores de un slot con las listas ya cargadas. */
    private fun refrescarOpciones(fila: FilaSlot) {
        fila.binding.dropCategoria.setSimpleItems(categorias.map { it.nombre.orEmpty() }.toTypedArray())
    }

    /** Muestra el selector correspondiente al tipo (prenda fija vs. categoría del pool). */
    private fun aplicarTipo(fila: FilaSlot, tipo: SlotDto.EjePrenda) {
        fila.tipo = tipo
        val esFija = tipo == SlotDto.EjePrenda.FIJA
        fila.binding.tarjetaElegirPrenda.isVisible = esFija
        // El desplegable de categoría del slot es redundante: el selector unificado filtra por categoría dentro.
        fila.binding.tilCategoria.isVisible = false
        fila.binding.botonEtiquetas.isVisible = !esFija
    }

    /** Refleja en la tarjeta la prenda fija elegida (foto + precio); si no hay, invita a elegir. */
    private fun pintarPrendaFija(fila: FilaSlot) {
        val p = fila.prenda
        val b = fila.binding
        if (p == null) {
            b.miniaturaPrenda.isVisible = false
            b.textoPrendaElegida.text = "Elegir prenda"
            b.detallePrendaElegida.isVisible = false
            return
        }
        b.miniaturaPrenda.isVisible = true
        b.miniaturaPrenda.cargarFoto(p.fotoUrl)
        b.textoPrendaElegida.text = p.nombre.orEmpty()
        val precio = (p.precioRenta ?: p.precioVenta).comoPrecio()
        b.detallePrendaElegida.isVisible = precio != null
        b.detallePrendaElegida.text = precio.orEmpty()
    }

    /**
     * Selector unificado del catálogo para una pieza: delega en [SelectorCatalogo] (grilla con foto/precio/
     * stock, buscador y filtros compactos). Modo [multiple] = parte personalizable (varias con "Listo"); si
     * no, prenda fija (un toque elige). El mismo componente lo usa el punto de venta.
     */
    private fun abrirSelectorCatalogo(fila: FilaSlot, multiple: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val catalogo = vm.catalogo(null)
            if (catalogo.isEmpty()) {
                mostrarMensaje("No hay prendas para elegir.")
                return@launch
            }
            com.costumi.app.ui.common.SelectorCatalogo.abrirPrendas(
                fragment = this@DisfrazFormFragment,
                catalogo = catalogo,
                categorias = categorias,
                etiquetas = etiquetasDisponibles,
                multiple = multiple,
                titulo = if (multiple) "Elegir elementos" else "Elegir prenda",
                yaElegidas = if (multiple) fila.opciones.mapNotNull { it.id }.toSet() else setOfNotNull(fila.prenda?.id),
                onElegir = { prenda ->
                    // La tarjeta necesita la PrendaResponse (con sus valores para el sugerido): se mapea por id.
                    fila.prenda = prendas.firstOrNull { it.id == prenda.id }
                    pintarPrendaFija(fila)
                    recomputarSugerido()
                },
                onConfirmar = { seleccion ->
                    fila.opciones.clear()
                    fila.opciones.addAll(seleccion)
                    actualizarEtiquetaOpciones(fila)
                    recomputarSugerido()
                },
            )
        }
    }

    /** Refresca el texto del botón de una parte personalizable con cuántos elementos se eligieron. */
    private fun actualizarEtiquetaOpciones(fila: FilaSlot) {
        fila.binding.botonEtiquetas.text =
            if (fila.opciones.isEmpty()) "Elegir elementos" else "Elementos elegidos: ${fila.opciones.size}"
    }

    /**
     * Recalcula, en vivo, el precio sugerido del disfraz según los elementos puestos: suma por pieza el
     * precio de la prenda fija (directo), o el mín–máx de las opciones de una parte personalizable. Es una
     * sugerencia (no reemplaza el precio final que fije el dueño).
     */
    private fun recomputarSugerido() {
        val b = _binding ?: return
        var rentaMin = BigDecimal.ZERO
        var rentaMax = BigDecimal.ZERO
        var ventaMin = BigDecimal.ZERO
        var ventaMax = BigDecimal.ZERO
        var danoMin = BigDecimal.ZERO
        var danoMax = BigDecimal.ZERO
        var reposicionMin = BigDecimal.ZERO
        var reposicionMax = BigDecimal.ZERO
        for (f in filas) {
            if (f.tipo == SlotDto.EjePrenda.FIJA) {
                val p = f.prenda ?: continue
                (p.precioRenta ?: BigDecimal.ZERO).let { rentaMin = rentaMin.add(it); rentaMax = rentaMax.add(it) }
                (p.precioVenta ?: BigDecimal.ZERO).let { ventaMin = ventaMin.add(it); ventaMax = ventaMax.add(it) }
                (p.valorDano ?: BigDecimal.ZERO).let { danoMin = danoMin.add(it); danoMax = danoMax.add(it) }
                (p.valorReposicion ?: BigDecimal.ZERO).let {
                    reposicionMin = reposicionMin.add(it); reposicionMax = reposicionMax.add(it)
                }
            } else {
                sumarRango(f.opciones.mapNotNull { it.precioRenta }) { lo, hi ->
                    rentaMin = rentaMin.add(lo); rentaMax = rentaMax.add(hi)
                }
                sumarRango(f.opciones.mapNotNull { it.precioVenta }) { lo, hi ->
                    ventaMin = ventaMin.add(lo); ventaMax = ventaMax.add(hi)
                }
                sumarRango(f.opciones.mapNotNull { it.valorDano }) { lo, hi ->
                    danoMin = danoMin.add(lo); danoMax = danoMax.add(hi)
                }
                sumarRango(f.opciones.mapNotNull { it.valorReposicion }) { lo, hi ->
                    reposicionMin = reposicionMin.add(lo); reposicionMax = reposicionMax.add(hi)
                }
            }
        }
        val partes = mutableListOf<String>()
        if (rentaMax.signum() > 0) partes.add("Renta ${rango(rentaMin, rentaMax)}")
        if (ventaMax.signum() > 0) partes.add("Venta ${rango(ventaMin, ventaMax)}")
        b.sugeridos.isVisible = partes.isNotEmpty()
        if (partes.isNotEmpty()) {
            b.sugeridos.text = "Sugerido (según los elementos): " + partes.joinToString(" · ")
        }
        // Multa sugerida por tipo (daño y reposición/pérdida): rango según los elementos.
        val multas = mutableListOf<String>()
        if (danoMax.signum() > 0) multas.add("daño ${rango(danoMin, danoMax)}")
        if (reposicionMax.signum() > 0) multas.add("reposición ${rango(reposicionMin, reposicionMax)}")
        b.multaSugerida.isVisible = multas.isNotEmpty()
        if (multas.isNotEmpty()) {
            b.multaSugerida.text = "Multa sugerida: " + multas.joinToString(" · ")
        }
    }

    /** Suma al acumulador el mín y el máx de una lista de valores (si no está vacía). */
    private inline fun sumarRango(valores: List<BigDecimal>, acumular: (BigDecimal, BigDecimal) -> Unit) {
        if (valores.isEmpty()) return
        acumular(valores.reduce { a, c -> a.min(c) }, valores.reduce { a, c -> a.max(c) })
    }

    private fun rango(min: BigDecimal, max: BigDecimal): String =
        if (min.compareTo(max) == 0) (min.comoPrecio() ?: "$0")
        else "${min.comoPrecio() ?: "$0"} – ${max.comoPrecio() ?: "$0"}"

    private fun renumerar() {
        filas.forEachIndexed { i, f -> f.binding.tituloSlot.text = "Pieza ${i + 1}" }
    }

    private fun guardar() {
        val nombre = binding.editNombre.text?.toString()?.trim().orEmpty()
        if (nombre.isBlank()) {
            binding.editNombre.error = "Ingresa un nombre"
            return
        }
        if (filas.isEmpty()) {
            mostrarMensaje("Agrega al menos una pieza")
            return
        }
        val slots = filas.mapIndexed { i, f ->
            val nombreSlot = f.binding.editSlotNombre.text?.toString()?.trim()?.ifBlank { null } ?: "Pieza ${i + 1}"
            val opcional = f.binding.switchOpcional.isChecked
            if (f.tipo == SlotDto.EjePrenda.FIJA) {
                val prenda = f.prenda
                if (prenda?.id == null) { mostrarMensaje("La pieza ${i + 1} no tiene prenda seleccionada"); return }
                SlotDto(ejePrenda = SlotDto.EjePrenda.FIJA, orden = i + 1, nombre = nombreSlot,
                    prendaFijaId = prenda.id, pool = null, prendasOpcion = null, opcional = opcional)
            } else {
                if (f.opciones.isEmpty()) {
                    mostrarMensaje("La pieza ${i + 1} (personalizable) no tiene elementos elegidos")
                    return
                }
                SlotDto(
                    ejePrenda = SlotDto.EjePrenda.PERSONALIZABLE, orden = i + 1, nombre = nombreSlot,
                    prendaFijaId = null, pool = null, prendasOpcion = f.opciones.mapNotNull { it.id },
                    opcional = opcional,
                )
            }
        }
        val precio = binding.editPrecio.text?.toString()?.trim()?.replace(",", ".")?.toBigDecimalOrNull()
        val precioVenta = binding.editPrecioVenta.text?.toString()?.trim()?.replace(",", ".")?.toBigDecimalOrNull()
        vm.guardar(
            CrearDisfrazRequest(
                nombre = nombre,
                categoriaId = categoriaDisfraz?.id,
                tipo = tipoDisfraz,
                // Solo se manda el precio de la operación habilitada (el otro no aplica).
                precioRentaGeneral = precio.takeIf { tipoDisfraz != CrearDisfrazRequest.Tipo.VENTA },
                precioVentaGeneral = precioVenta.takeIf { tipoDisfraz != CrearDisfrazRequest.Tipo.RENTA },
                slots = slots,
            ),
            fotoBytes = fotoBytes,
            fotoMime = fotoMime,
            fotoNombre = fotoNombre,
        )
    }

    /** Lee la imagen elegida (en IO, sin crashear), la retiene para subirla al guardar y muestra la vista previa. */
    private fun elegirFoto(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val datos = requireContext().leerBytesDeImagen(uri)
            if (datos == null) {
                mostrarMensaje("No se pudo leer la imagen, intentá de nuevo")
                return@launch
            }
            fotoBytes = datos.first
            fotoMime = datos.second
            fotoNombre = "disfraz.${extensionDeImagen(datos.second)}"
            binding.foto.isVisible = true
            binding.foto.setImageURI(uri)
            binding.botonFoto.text = "Cambiar foto"
        }
    }

    override fun onDestroyView() {
        filas.clear()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_ID = "disfrazId"
        const val RESULT_GUARDADO = "disfraz_guardado"
        private const val SIN_CATEGORIA = "Sin categoria"

        /** El backend deduce el tipo mirando las piezas; es lo que se ofrece por defecto al crear. */
        private const val AUTOMATICO = "Automatico (segun las piezas)"

        fun args(id: UUID) = bundleOf(ARG_ID to id.toString())
    }
}
