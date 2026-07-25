package com.costumi.app.ui.gestion.ventas

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.FragmentVentaPosBinding
import com.costumi.app.databinding.ItemPedidoDisfrazBinding
import com.costumi.app.ui.common.LineasDeArticulos
import com.costumi.app.ui.common.ListaBuscable
import com.costumi.app.ui.common.OpcionBuscable
import com.costumi.app.ui.common.SelectorCatalogo
import com.costumi.app.ui.common.SelectorDisfraces
import com.costumi.app.ui.gestion.disfraces.DisfrazAsignarFragment
import com.costumi.app.ui.gestion.disfraces.PedidoDisfracesStore
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.LineaPrendaVentaDto
import com.costumi.apiclient.models.LineaVentaRequest
import com.costumi.apiclient.models.SucursalResponse
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** POS — registrar una venta: sucursal, cliente opcional y un TICKET de artículos con total en vivo. */
@AndroidEntryPoint
class VentaPosFragment : Fragment(R.layout.fragment_venta_pos) {

    private val vm: VentaPosViewModel by viewModels()
    private var _binding: FragmentVentaPosBinding? = null
    private val binding get() = _binding!!

    /** Las líneas del ticket viven en el componente compartido con "Nueva renta". */
    private var lineas: LineasDeArticulos? = null
    private var lineasGuardadas: List<LineasDeArticulos.Guardada>? = null

    private var sucursales: List<SucursalResponse> = emptyList()
    private var clientes: List<ClienteResponse> = emptyList()
    private var disfraces: List<DisfrazResponse> = emptyList()
    private var categorias: List<com.costumi.apiclient.models.CategoriaResponse> = emptyList()
    private var etiquetas: List<com.costumi.app.data.repo.TipoConValores> = emptyList()
    private var sucursalSeleccionada: SucursalResponse? = null
    private var clienteSeleccionado: ClienteResponse? = null
    private var preparado = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentVentaPosBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.botonAgregar.setOnClickListener { abrirPickerProducto() }
        binding.botonAgregarDisfraz.setOnClickListener { elegirDisfraz() }
        binding.botonConfirmar.setOnClickListener { confirmar() }
        lineas = LineasDeArticulos(this, binding.contenedorLineas, "Precio unitario") { recalcular() }

        observar(vm.datos) { estado ->
            binding.stateView.mostrar(estado, vacio = "Crea prendas y sucursales antes de vender.") { datos ->
                sucursales = datos.sucursales
                clientes = datos.clientes
                disfraces = datos.disfraces
                categorias = datos.categorias
                etiquetas = datos.etiquetas
                if (!preparado) {
                    preparado = true
                    configurarSucursal()
                    configurarCliente()
                }
                // Al volver de configurar un disfraz, restaura las líneas que había.
                if (lineas?.estaVacio() == true) {
                    lineasGuardadas?.let { lineas?.restaurar(it) }
                    lineasGuardadas = null
                }
            }
        }
        observar(vm.disfracesPedido) { pintarDisfraces(it) }
        observar(vm.registrando) { registrando ->
            binding.botonConfirmar.isEnabled = !registrando
            binding.botonConfirmar.text = if (registrando) "Cobrando..." else "Cobrar"
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoPos.Registrada -> {
                    setFragmentResult(RESULT_REGISTRADA, Bundle.EMPTY)
                    val msg = evento.total?.let { "Venta registrada por ${it.comoPrecio()}." } ?: "Venta registrada."
                    mostrarMensaje(msg)
                    findNavController().popBackStack()
                }
                is EventoPos.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun configurarSucursal() {
        binding.dropSucursal.setSimpleItems(sucursales.map { it.nombre.orEmpty() }.toTypedArray())
        sucursalSeleccionada = sucursales.singleOrNull()
        sucursalSeleccionada?.let { binding.dropSucursal.setText(it.nombre.orEmpty(), false) }
        binding.dropSucursal.setOnItemClickListener { _, _, pos, _ -> sucursalSeleccionada = sucursales.getOrNull(pos) }
    }

    /** Cliente por SELECTOR CON BUSCADOR (no un desplegable): con muchos clientes, escribir es lo rápido. */
    private fun configurarCliente() {
        binding.dropCliente.isFocusableInTouchMode = false
        binding.dropCliente.isFocusable = false
        binding.dropCliente.setText("Sin cliente", false)
        binding.dropCliente.setOnClickListener { abrirSelectorCliente() }
    }

    private fun abrirSelectorCliente() {
        val opciones = listOf(OpcionBuscable(SIN_CLIENTE, "Sin cliente")) +
            clientes.mapNotNull { c -> c.id?.let { OpcionBuscable(it.toString(), c.nombre.orEmpty().ifBlank { "Cliente" }) } }
        val actual = clienteSeleccionado?.id?.toString() ?: SIN_CLIENTE
        ListaBuscable.unaOpcion(requireContext(), "Elegir cliente", opciones, actual) { id ->
            clienteSeleccionado = if (id == null || id == SIN_CLIENTE) null else clientes.firstOrNull { it.id?.toString() == id }
            binding.dropCliente.setText(clienteSeleccionado?.nombre?.ifBlank { "Cliente" } ?: "Sin cliente", false)
        }
    }

    /**
     * Buscador VISUAL de artículos: el MISMO selector de catálogo que usan armar disfraz y Nueva renta
     * (grilla con foto, precio, stock, buscador y filtros compactos). Tocar uno lo agrega al ticket.
     */
    private fun abrirPickerProducto() {
        viewLifecycleOwner.lifecycleScope.launch {
            val catalogo = vm.catalogo()
            if (catalogo.isEmpty()) {
                mostrarMensaje("No hay articulos para vender.")
                return@launch
            }
            SelectorCatalogo.abrirPrendas(
                fragment = this@VentaPosFragment,
                catalogo = catalogo,
                categorias = categorias,
                etiquetas = etiquetas,
                multiple = false,
                titulo = "Agregar articulo",
                onElegir = { prenda ->
                    val id = prenda.id ?: return@abrirPrendas
                    lineas?.agregarOSumar(
                        prendaId = id,
                        nombre = prenda.nombre.orEmpty().ifBlank { "Articulo" },
                        fotoUrl = prenda.fotoUrl,
                        precio = prenda.precioVenta?.toPlainString(),
                        stock = prenda.unidadesDisponibles?.toInt(),
                    )
                },
            )
        }
    }

    /** Picker VISUAL de disfraces (el mismo que en Nueva renta): al elegir uno se configuran sus piezas. */
    private fun elegirDisfraz() {
        if (disfraces.isEmpty()) {
            mostrarMensaje("No hay disfraces para agregar.")
            return
        }
        SelectorDisfraces.abrir(this, disfraces) { disfraz ->
            val id = disfraz.id ?: return@abrir
            findNavController().navigate(
                R.id.disfrazAsignarFragment,
                DisfrazAsignarFragment.argsPedido(id, disfraz.nombre ?: "Disfraz"),
            )
        }
    }

    private fun pintarDisfraces(items: List<PedidoDisfracesStore.Item>) {
        val c = _binding?.contenedorDisfraces ?: return
        c.removeAllViews()
        items.forEachIndexed { indice, item ->
            val fila = ItemPedidoDisfrazBinding.inflate(layoutInflater, c, false)
            fila.texto.text = "${item.nombre}  ·  x${item.cantidad}"
            fila.botonQuitar.setOnClickListener { vm.quitarDisfraz(indice) }
            c.addView(fila.root)
        }
    }

    private fun recalcular() {
        val total = lineas?.total() ?: java.math.BigDecimal.ZERO
        _binding?.total?.text = "Total: ${total.comoPrecio() ?: "$0"}"
    }

    private fun confirmar() {
        val sucursal = sucursalSeleccionada?.id
        if (sucursal == null) {
            mostrarMensaje("Selecciona una sucursal")
            return
        }
        val tieneDisfraces = vm.disfracesPedido.value.isNotEmpty()
        if (tieneDisfraces && clienteSeleccionado == null) {
            mostrarMensaje("Para vender un disfraz, elige un cliente.")
            return
        }
        val lineasVenta = mutableListOf<LineaVentaRequest>()
        val lineasMixto = mutableListOf<LineaPrendaVentaDto>()
        for (linea in lineas?.lineas.orEmpty()) {
            val precio = linea.precio
            if (precio == null) { mostrarMensaje("Precio invalido en \"${linea.nombre}\""); return }
            lineasVenta.add(LineaVentaRequest(prendaId = linea.prendaId, precioUnitario = precio, cantidad = linea.cantidad))
            lineasMixto.add(LineaPrendaVentaDto(prendaId = linea.prendaId, precioUnitario = precio, cantidad = linea.cantidad))
        }
        if (lineasVenta.isEmpty() && !tieneDisfraces) {
            mostrarMensaje("Agrega al menos un articulo o un disfraz")
            return
        }
        if (tieneDisfraces) {
            vm.registrarMixto(sucursal, clienteSeleccionado?.id, lineasMixto, vm.itemsDisfrazDelPedido())
        } else {
            vm.registrar(sucursal, clienteSeleccionado?.id, lineasVenta, descuento = null)
        }
    }

    override fun onDestroyView() {
        // Guarda las líneas antes de destruir la vista (al ir a configurar un disfraz) para restaurarlas.
        lineasGuardadas = lineas?.guardar()
        lineas = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_REGISTRADA = "venta_registrada"
        private const val SIN_CLIENTE = "__sin_cliente__"
    }
}
