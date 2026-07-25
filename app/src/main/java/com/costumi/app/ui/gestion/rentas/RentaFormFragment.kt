package com.costumi.app.ui.gestion.rentas

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.FragmentRentaFormBinding
import com.costumi.app.databinding.ItemPedidoDisfrazBinding
import com.costumi.app.ui.common.LineasDeArticulos
import com.costumi.app.ui.common.ListaBuscable
import com.costumi.app.ui.common.OpcionBuscable
import com.costumi.app.ui.common.SelectorCatalogo
import com.costumi.app.ui.common.SelectorDePeriodo
import com.costumi.app.ui.common.SelectorDisfraces
import com.costumi.app.ui.gestion.disfraces.DisfrazAsignarFragment
import com.costumi.app.ui.gestion.disfraces.PedidoDisfracesStore
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.ClienteResponse
import com.costumi.apiclient.models.DisfrazResponse
import com.costumi.apiclient.models.LineaPrendaRentaDto
import com.costumi.apiclient.models.LineaRentaDto
import com.costumi.apiclient.models.SucursalResponse
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Alta de renta. Gemela del punto de venta: mismos componentes (catálogo visual, stepper de cantidad,
 * selector de disfraces) y, propio de la renta, el periodo con calendario de rango.
 */
@AndroidEntryPoint
class RentaFormFragment : Fragment(R.layout.fragment_renta_form) {

    private val vm: RentaFormViewModel by viewModels()
    private var _binding: FragmentRentaFormBinding? = null
    private val binding get() = _binding!!

    private var lineas: LineasDeArticulos? = null
    private var lineasGuardadas: List<LineasDeArticulos.Guardada>? = null
    private var periodo: SelectorDePeriodo? = null
    /** El periodo elegido sobrevive a ir a configurar un disfraz y volver. */
    private var periodoGuardado: Pair<LocalDate, LocalDate>? = null

    private var sucursales: List<SucursalResponse> = emptyList()
    private var clientes: List<ClienteResponse> = emptyList()
    private var disfraces: List<DisfrazResponse> = emptyList()
    private var categorias: List<com.costumi.apiclient.models.CategoriaResponse> = emptyList()
    private var etiquetas: List<com.costumi.app.data.repo.TipoConValores> = emptyList()
    private var sucursalSeleccionada: SucursalResponse? = null
    private var clienteSeleccionado: ClienteResponse? = null
    private var preparado = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentRentaFormBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.botonConfirmar.setOnClickListener { confirmar() }
        binding.botonAgregar.setOnClickListener { abrirPickerPrenda() }
        binding.botonAgregarDisfraz.setOnClickListener { elegirDisfraz() }

        lineas = LineasDeArticulos(this, binding.contenedorLineas, "Precio por dia") { recalcular() }
        periodo = SelectorDePeriodo(this, binding.periodo, "Fechas de renta") { _, _ -> recalcular() }
        periodoGuardado?.let { (desde, hasta) -> periodo?.fijar(desde, hasta) }

        observar(vm.datos) { estado ->
            binding.stateView.mostrar(estado, vacio = "Crea clientes y prendas antes de rentar.") { datos ->
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
                recalcular()
            }
        }
        observar(vm.disfracesPedido) { pintarDisfraces(it) }
        observar(vm.registrando) { registrando ->
            binding.botonConfirmar.isEnabled = !registrando
            binding.botonConfirmar.text = if (registrando) "Registrando..." else "Registrar renta"
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoRentaForm.Registrada -> {
                    setFragmentResult(RESULT_REGISTRADA, Bundle.EMPTY)
                    val msg = evento.importe?.let { "Renta registrada por ${it.comoPrecio()}." }
                        ?: "Renta registrada."
                    mostrarMensaje(msg)
                    findNavController().popBackStack()
                }
                is EventoRentaForm.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun configurarSucursal() {
        binding.dropSucursal.setSimpleItems(sucursales.map { it.nombre.orEmpty() }.toTypedArray())
        sucursalSeleccionada = sucursales.singleOrNull()
        sucursalSeleccionada?.let { binding.dropSucursal.setText(it.nombre.orEmpty(), false) }
        binding.dropSucursal.setOnItemClickListener { _, _, pos, _ -> sucursalSeleccionada = sucursales.getOrNull(pos) }
    }

    /** Cliente por selector con buscador, igual que en el punto de venta (aquí es obligatorio). */
    private fun configurarCliente() {
        binding.dropCliente.isFocusableInTouchMode = false
        binding.dropCliente.isFocusable = false
        binding.dropCliente.setOnClickListener {
            val opciones = clientes.mapNotNull { c ->
                c.id?.let { OpcionBuscable(it.toString(), c.nombre.orEmpty().ifBlank { "Cliente" }) }
            }
            ListaBuscable.unaOpcion(
                requireContext(), "Elegir cliente", opciones, clienteSeleccionado?.id?.toString(),
            ) { id ->
                clienteSeleccionado = clientes.firstOrNull { it.id?.toString() == id }
                binding.dropCliente.setText(clienteSeleccionado?.nombre.orEmpty(), false)
            }
        }
    }

    /** El MISMO catálogo visual del punto de venta; aquí el precio que trae es el de renta por día. */
    private fun abrirPickerPrenda() {
        viewLifecycleOwner.lifecycleScope.launch {
            val catalogo = vm.catalogo()
            if (catalogo.isEmpty()) {
                mostrarMensaje("No hay prendas para rentar.")
                return@launch
            }
            SelectorCatalogo.abrirPrendas(
                fragment = this@RentaFormFragment,
                catalogo = catalogo,
                categorias = categorias,
                etiquetas = etiquetas,
                multiple = false,
                titulo = "Agregar prenda",
                onElegir = { prenda ->
                    val id = prenda.id ?: return@abrirPrendas
                    lineas?.agregarOSumar(
                        prendaId = id,
                        nombre = prenda.nombre.orEmpty().ifBlank { "Prenda" },
                        fotoUrl = prenda.fotoUrl,
                        precio = prenda.precioRenta?.toPlainString(),
                        stock = prenda.unidadesDisponibles?.toInt(),
                    )
                },
            )
        }
    }

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

    /** Dias del periodo (minimo 1): es el multiplicador de cada linea. */
    private fun dias(): Int {
        val desde = periodo?.retiro ?: return 1
        val hasta = periodo?.devolucion ?: return 1
        return ChronoUnit.DAYS.between(desde, hasta).toInt().coerceAtLeast(1)
    }

    private fun recalcular() {
        val b = _binding ?: return
        val cuenta = lineas?.lineas?.size ?: 0
        val dias = dias()
        lineas?.multiplicador = dias
        val total = lineas?.total() ?: BigDecimal.ZERO
        b.total.text = "Total: ${total.comoPrecio() ?: "$0"}"
        b.detalleTotal.text = when {
            cuenta == 0 -> "Agrega al menos una prenda o un disfraz"
            periodo?.completo != true -> "Falta elegir las fechas"
            else -> listOf(
                if (cuenta == 1) "1 prenda" else "$cuenta prendas",
                if (dias == 1) "1 dia" else "$dias dias",
            ).joinToString("  ·  ")
        }
    }

    private fun confirmar() {
        val sucursal = sucursalSeleccionada?.id ?: run { mostrarMensaje("Selecciona una sucursal"); return }
        val cliente = clienteSeleccionado?.id ?: run { mostrarMensaje("Selecciona un cliente"); return }
        val retiro = periodo?.retiro ?: run { mostrarMensaje("Elige las fechas de la renta"); return }
        val devolucion = periodo?.devolucion ?: run { mostrarMensaje("Elige las fechas de la renta"); return }

        val tieneDisfraces = vm.disfracesPedido.value.isNotEmpty()
        val lineasRenta = mutableListOf<LineaRentaDto>()
        val lineasMixto = mutableListOf<LineaPrendaRentaDto>()
        for (linea in lineas?.lineas.orEmpty()) {
            val precio = linea.precio
                ?: run { mostrarMensaje("Falta el precio por dia de \"${linea.nombre}\""); return }
            lineasRenta.add(LineaRentaDto(prendaId = linea.prendaId, precioPorDia = precio, cantidad = linea.cantidad))
            lineasMixto.add(LineaPrendaRentaDto(prendaId = linea.prendaId, precioPorDia = precio, cantidad = linea.cantidad))
        }
        if (lineasRenta.isEmpty() && !tieneDisfraces) {
            mostrarMensaje("Agrega al menos una prenda o un disfraz")
            return
        }
        // Ya no se usa depósito: la renta se paga directo; el daño/pérdida se cobran en la devolución.
        if (tieneDisfraces) {
            vm.registrarMixto(sucursal, cliente, retiro, devolucion, lineasMixto, vm.itemsDisfrazDelPedido())
        } else {
            vm.registrar(sucursal, cliente, retiro, devolucion, lineasRenta, deposito = null)
        }
    }

    override fun onDestroyView() {
        // Se guardan líneas y fechas antes de destruir la vista (p. ej. al ir a configurar un disfraz).
        lineasGuardadas = lineas?.guardar()
        periodoGuardado = periodo?.let { p ->
            val desde = p.retiro
            val hasta = p.devolucion
            if (desde != null && hasta != null) desde to hasta else null
        }
        lineas = null
        periodo = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_REGISTRADA = "renta_registrada"
    }
}
