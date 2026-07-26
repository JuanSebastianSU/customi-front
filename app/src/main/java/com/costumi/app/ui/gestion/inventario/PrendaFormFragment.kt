package com.costumi.app.ui.gestion.inventario

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.costumi.app.R
import com.costumi.app.databinding.FragmentPrendaFormBinding
import com.costumi.app.databinding.ItemEtiquetaSelectorBinding
import com.costumi.app.ui.cargarFoto
import com.costumi.app.ui.extensionDeImagen
import com.costumi.app.ui.leerBytesDeImagen
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.app.ui.soloImagenes
import com.costumi.apiclient.models.CategoriaResponse
import com.costumi.apiclient.models.CrearPrendaRequest
import com.costumi.apiclient.models.EtiquetaSeleccionadaDto
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.util.UUID

/** Alta/edición de una prenda. Sin argumento `id` = alta; con `id` = edición (categoría/tipo fijos). */
@AndroidEntryPoint
class PrendaFormFragment : Fragment(R.layout.fragment_prenda_form) {

    private val vm: PrendaFormViewModel by viewModels()
    private var _binding: FragmentPrendaFormBinding? = null
    private val binding get() = _binding!!

    private var categorias: List<CategoriaResponse> = emptyList()
    private var categoriaSeleccionada: CategoriaResponse? = null

    // Etiquetas (RF-2.7): tipos disponibles, los aplicables a la categoría elegida y la selección actual.
    private var etiquetasDisponibles: List<PrendaFormViewModel.TipoConValores> = emptyList()
    private var tiposAplicables: List<PrendaFormViewModel.TipoConValores> = emptyList()
    private val seleccionEtiquetas = mutableMapOf<UUID, UUID>()

    // Foto elegida (aún no subida): se sube al guardar, con el id resultante.
    private var fotoBytes: ByteArray? = null
    private var fotoMime: String? = null
    private var fotoNombre: String? = null

    /** Selector de imagen moderno; al elegir, la guarda localmente y muestra la vista previa. */
    private val seleccionarFoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { elegirFoto(it) }
    }

    private var tipoSeleccionado: CrearPrendaRequest.TipoArticulo? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPrendaFormBinding.bind(view)

        binding.toolbar.title = if (vm.esEdicion) "Editar prenda" else "Nueva prenda"
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        configurarTipo()
        prefill()
        bloquearCategoriaYTipo()
        cargarSeleccionEtiquetasInicial()

        binding.dropCategoria.setOnItemClickListener { _, _, pos, _ ->
            categoriaSeleccionada = categorias.getOrNull(pos)
            refrescarEtiquetas()
        }
        binding.botonGuardar.setOnClickListener { guardar() }

        // La foto se puede agregar tanto al crear como al editar (se sube al guardar).
        binding.botonFoto.setOnClickListener { seleccionarFoto.launch(soloImagenes) }

        observar(vm.categorias) { estado ->
            binding.stateView.mostrar(estado, vacio = "No hay categorias. Crea una primero.") { lista ->
                categorias = lista
                binding.dropCategoria.setSimpleItems(lista.map { it.nombre.orEmpty() }.toTypedArray())
                preseleccionarCategoria()
            }
        }
        observar(vm.etiquetasDisponibles) { lista ->
            etiquetasDisponibles = lista
            refrescarEtiquetas()
        }
        observar(vm.guardando) { guardando ->
            binding.botonGuardar.isEnabled = !guardando
            binding.botonGuardar.text = if (guardando) "Guardando..." else "Guardar"
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoForm.Guardada -> {
                    evento.avisoFoto?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
                    setFragmentResult(RESULT_GUARDADA, Bundle.EMPTY)
                    findNavController().popBackStack()
                }
                is EventoForm.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    /** Lee los bytes de la imagen elegida (en IO, sin crashear), la retiene para subirla al guardar y muestra la vista previa. */
    private fun elegirFoto(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val datos = requireContext().leerBytesDeImagen(uri)
            if (datos == null) {
                mostrarMensaje("No se pudo leer la imagen, intentá de nuevo")
                return@launch
            }
            fotoBytes = datos.first
            fotoMime = datos.second
            fotoNombre = "foto.${extensionDeImagen(datos.second)}"
            binding.foto.isVisible = true
            binding.foto.setImageURI(uri)
            binding.botonFoto.text = "Cambiar foto"
        }
    }

    /**
     * El tipo se elige de un toque y ademas MANDA sobre los precios: una prenda que solo se vende no
     * tiene por que pedir precio de renta por dia (antes se pedian los cinco siempre).
     */
    private fun configurarTipo() {
        binding.grupoTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            tipoSeleccionado = when (checkedId) {
                R.id.botonRentar -> CrearPrendaRequest.TipoArticulo.RENTA
                R.id.botonVender -> CrearPrendaRequest.TipoArticulo.VENTA
                else -> CrearPrendaRequest.TipoArticulo.AMBOS
            }
            aplicarTipoAPrecios()
        }
    }

    /**
     * En edicion el backend no permite cambiar categoria ni tipo. Se bloquea DESPUES de precargar: un
     * boton deshabilitado ignora el check(), y el tipo quedaba sin marcar (parecia que no habia ninguno).
     */
    private fun bloquearCategoriaYTipo() {
        if (!vm.esEdicion) return
        binding.tilCategoria.isEnabled = false
        // No se deshabilitan los botones: un MaterialButton deshabilitado deja de PINTARSE como
        // seleccionado y el tipo parecia vacio. Se dejan visibles pero sin responder al toque.
        for (i in 0 until binding.grupoTipo.childCount) {
            binding.grupoTipo.getChildAt(i).apply {
                isClickable = false
                isFocusable = false
            }
        }
        binding.avisoTipoFijo.isVisible = true
    }

    /** Muestra solo los precios que aplican al tipo elegido. */
    private fun aplicarTipoAPrecios() {
        val tipo = tipoSeleccionado
        binding.tilPrecioRenta.isVisible =
            tipo == null || tipo == CrearPrendaRequest.TipoArticulo.RENTA || tipo == CrearPrendaRequest.TipoArticulo.AMBOS
        binding.tilPrecioVenta.isVisible =
            tipo == null || tipo == CrearPrendaRequest.TipoArticulo.VENTA || tipo == CrearPrendaRequest.TipoArticulo.AMBOS
    }

    private fun prefill() {
        val a = arguments ?: return
        binding.editNombre.setText(a.getString(ARG_NOMBRE).orEmpty())
        a.getString(ARG_PRECIO_VENTA)?.let { binding.editPrecioVenta.setText(it) }
        a.getString(ARG_PRECIO_RENTA)?.let { binding.editPrecioRenta.setText(it) }
        a.getString(ARG_COSTO)?.let { binding.editCosto.setText(it) }
        a.getString(ARG_VALOR_REPOSICION)?.let { binding.editValorReposicion.setText(it) }
        a.getString(ARG_VALOR_DANO)?.let { binding.editValorDano.setText(it) }
        a.getString(ARG_FOTO_URL)?.takeIf { it.isNotBlank() }?.let {
            binding.foto.isVisible = true
            binding.foto.cargarFoto(it)
            binding.botonFoto.text = "Cambiar foto"
        }

        a.getString(ARG_TIPO)?.let { valor ->
            tipoSeleccionado = runCatching { CrearPrendaRequest.TipoArticulo.valueOf(valor) }.getOrNull()
            when (tipoSeleccionado) {
                CrearPrendaRequest.TipoArticulo.RENTA -> binding.grupoTipo.check(R.id.botonRentar)
                CrearPrendaRequest.TipoArticulo.VENTA -> binding.grupoTipo.check(R.id.botonVender)
                CrearPrendaRequest.TipoArticulo.AMBOS -> binding.grupoTipo.check(R.id.botonAmbos)
                null -> Unit
            }
        }
        aplicarTipoAPrecios()
    }

    private fun preseleccionarCategoria() {
        val catId = arguments?.getString(ARG_CATEGORIA_ID) ?: return
        categorias.firstOrNull { it.id?.toString() == catId }?.let { cat ->
            categoriaSeleccionada = cat
            binding.dropCategoria.setText(cat.nombre.orEmpty(), false)
            refrescarEtiquetas()
        }
    }

    /** Carga (en edición) las etiquetas actuales de la prenda para no perderlas al volver a guardar. */
    private fun cargarSeleccionEtiquetasInicial() {
        val guardadas = arguments?.getStringArrayList(ARG_ETIQUETAS) ?: return
        guardadas.forEach { par ->
            val partes = par.split("|")
            if (partes.size == 2) {
                runCatching { UUID.fromString(partes[0]) to UUID.fromString(partes[1]) }
                    .getOrNull()?.let { seleccionEtiquetas[it.first] = it.second }
            }
        }
    }

    /**
     * Reconstruye los selectores de etiqueta según la categoría elegida: muestra un desplegable por
     * cada tipo de etiqueta que aplica (RF-2.7), con "(sin asignar)" + sus valores, y conserva la
     * selección previa. Sin categoría o sin tipos aplicables, oculta la sección.
     */
    private fun refrescarEtiquetas() {
        val contenedor = _binding?.contenedorEtiquetas ?: return
        contenedor.removeAllViews()

        val catId = categoriaSeleccionada?.id
            ?: arguments?.getString(ARG_CATEGORIA_ID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }

        tiposAplicables = if (catId == null) emptyList() else etiquetasDisponibles.filter { tv ->
            val cats = tv.tipo.categoriasQueAplica
            cats.isNullOrEmpty() || cats.contains(catId)
        }

        val hay = tiposAplicables.isNotEmpty()
        binding.tvEtiquetas.isVisible = hay
        binding.tvEtiquetasAyuda.isVisible = hay
        if (!hay) return

        tiposAplicables.forEach { tv ->
            val tipoId = tv.tipo.id ?: return@forEach
            val item = ItemEtiquetaSelectorBinding.inflate(layoutInflater, contenedor, false)
            item.tilEtiqueta.hint = tv.tipo.nombre.orEmpty()

            val opciones = listOf(SIN_ASIGNAR) + tv.valores.map { it.valor.orEmpty() }
            item.dropEtiqueta.setSimpleItems(opciones.toTypedArray())

            seleccionEtiquetas[tipoId]?.let { valorId ->
                tv.valores.firstOrNull { it.id == valorId }?.let { valor ->
                    item.dropEtiqueta.setText(valor.valor.orEmpty(), false)
                }
            }

            item.dropEtiqueta.setOnItemClickListener { _, _, pos, _ ->
                if (pos == 0) {
                    seleccionEtiquetas.remove(tipoId)
                } else {
                    tv.valores.getOrNull(pos - 1)?.id?.let { seleccionEtiquetas[tipoId] = it }
                }
            }
            contenedor.addView(item.root)
        }
    }

    private fun guardar() {
        val nombre = binding.editNombre.text?.toString()?.trim().orEmpty()
        if (nombre.isBlank()) {
            binding.editNombre.error = "Ingresa un nombre"
            return
        }
        if (!vm.esEdicion && categoriaSeleccionada == null) {
            mostrarMensaje("Selecciona una categoria")
            return
        }
        if (!vm.esEdicion && tipoSeleccionado == null) {
            mostrarMensaje("Selecciona el tipo de articulo")
            return
        }
        val etiquetas = tiposAplicables.mapNotNull { tv ->
            val tipoId = tv.tipo.id ?: return@mapNotNull null
            val valorId = seleccionEtiquetas[tipoId] ?: return@mapNotNull null
            EtiquetaSeleccionadaDto(tipoEtiquetaId = tipoId, valorEtiquetaId = valorId)
        }
        vm.guardar(
            nombre = nombre,
            categoriaId = categoriaSeleccionada?.id,
            tipo = tipoSeleccionado,
            precioRenta = decimalDe(binding.editPrecioRenta),
            precioVenta = decimalDe(binding.editPrecioVenta),
            costo = decimalDe(binding.editCosto),
            deposito = null, // ya no se usa depósito: se paga directo; daño/pérdida vía valorDano/valorReposicion
            valorReposicion = decimalDe(binding.editValorReposicion),
            valorDano = decimalDe(binding.editValorDano),
            etiquetas = etiquetas,
            fotoBytes = fotoBytes,
            fotoMime = fotoMime,
            fotoNombre = fotoNombre,
        )
    }

    private fun decimalDe(campo: EditText): BigDecimal? {
        val texto = campo.text?.toString()?.trim()?.replace(",", ".").orEmpty()
        if (texto.isBlank()) return null
        return texto.toBigDecimalOrNull()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_NOMBRE = "nombre"
        const val ARG_CATEGORIA_ID = "categoriaId"
        const val ARG_TIPO = "tipo"
        const val ARG_PRECIO_VENTA = "precioVenta"
        const val ARG_PRECIO_RENTA = "precioRenta"
        const val ARG_COSTO = "costo"
        const val ARG_DEPOSITO = "deposito"
        const val ARG_VALOR_REPOSICION = "valorReposicion"
        const val ARG_VALOR_DANO = "valorDano"
        const val ARG_FOTO_URL = "fotoUrl"
        const val ARG_ETIQUETAS = "etiquetas"

        const val RESULT_GUARDADA = "prenda_guardada"

        private const val SIN_ASIGNAR = "(sin asignar)"

        /** Bundle para editar una prenda: precarga todos los campos conocidos, incluidas sus etiquetas. */
        fun argsEditar(
            id: UUID,
            nombre: String?,
            categoriaId: UUID?,
            tipo: String?,
            precioVenta: BigDecimal?,
            precioRenta: BigDecimal?,
            costo: BigDecimal?,
            deposito: BigDecimal?,
            valorReposicion: BigDecimal?,
            valorDano: BigDecimal?,
            fotoUrl: String? = null,
            etiquetas: List<EtiquetaSeleccionadaDto> = emptyList(),
        ) = Bundle().apply {
            putString(ARG_ID, id.toString())
            putString(ARG_NOMBRE, nombre)
            putString(ARG_CATEGORIA_ID, categoriaId?.toString())
            putString(ARG_TIPO, tipo)
            putString(ARG_PRECIO_VENTA, precioVenta?.toPlainString())
            putString(ARG_PRECIO_RENTA, precioRenta?.toPlainString())
            putString(ARG_COSTO, costo?.toPlainString())
            putString(ARG_DEPOSITO, deposito?.toPlainString())
            putString(ARG_VALOR_REPOSICION, valorReposicion?.toPlainString())
            putString(ARG_VALOR_DANO, valorDano?.toPlainString())
            putString(ARG_FOTO_URL, fotoUrl)
            putStringArrayList(
                ARG_ETIQUETAS,
                ArrayList(etiquetas.map { "${it.tipoEtiquetaId}|${it.valorEtiquetaId}" }),
            )
        }
    }
}
