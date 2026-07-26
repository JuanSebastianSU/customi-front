package com.costumi.app.ui.gestion.rentas

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import java.io.File
import com.costumi.app.R
import com.costumi.app.databinding.FragmentRentasBinding
import com.costumi.app.core.comoPrecio
import com.costumi.app.ui.common.aLocalDate
import com.costumi.app.ui.common.comoDiaMes
import com.costumi.app.ui.common.enMillisUtc
import com.costumi.app.ui.common.periodoLegible
import com.costumi.app.ui.gestion.LineaDesglose
import com.costumi.app.ui.gestion.devoluciones.DevolucionFormFragment
import com.costumi.app.ui.gestion.inventario.PrendasLoadStateAdapter
import com.costumi.app.ui.gestion.mostrarDesglose
import com.costumi.app.ui.gestion.pagos.PagoConceptoFragment
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.alBuscar
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.RentaResponse
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

/** Rentas — listado paginado con alta y ciclo de estados (entregar/devolver/cerrar/cancelar/extender). */
@AndroidEntryPoint
class RentasFragment : Fragment(R.layout.fragment_rentas) {

    private val vm: RentasViewModel by viewModels()
    private var _binding: FragmentRentasBinding? = null
    private val binding get() = _binding!!
    private val adapter = RentaAdapter(
        alTocar = { mostrarDesgloseRenta(it) },
        alAccionar = { renta, accion -> ejecutar(renta, accion) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentRentasBinding.bind(view)
        binding.barraBusqueda.tilBuscar.hint = "Buscar por codigo de retiro (R-...)"
        binding.barraBusqueda.editBuscar.alBuscar { vm.buscar(it) }
        binding.lista.adapter = adapter.withLoadStateFooter(PrendasLoadStateAdapter { adapter.retry() })

        // Si se llegó desde la ficha de un cliente (atajo), se acota a sus rentas y se muestra de quién es.
        vm.clienteNombre?.takeIf { it.isNotBlank() }?.let { nombre ->
            binding.toolbar.subtitle = "Cliente: $nombre"
            binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
            binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        }

        setFragmentResultListener(RentaFormFragment.RESULT_REGISTRADA) { _, _ -> adapter.refresh() }
        setFragmentResultListener(DevolucionFormFragment.RESULT_REGISTRADA) { _, _ -> adapter.refresh() }
        setFragmentResultListener(PagoConceptoFragment.RESULT_COBRADO) { _, _ -> adapter.refresh() }
        binding.fabNueva.setOnClickListener { findNavController().navigate(R.id.rentaFormFragment) }

        pintarChipsEstado()

        observar(vm.rentas) { adapter.submitData(viewLifecycleOwner.lifecycle, it) }
        observar(adapter.loadStateFlow) { estados -> pintarEstado(estados.refresh) }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoRenta.Info -> { mostrarMensaje(evento.mensaje); adapter.refresh() }
                is EventoRenta.Error -> mostrarMensaje(evento.mensaje)
                is EventoRenta.Contrato -> abrirPdf(evento.bytes)
            }
        }
    }

    /** Chips de filtro por bandeja/estado (server-side): Todas / Por entregar / Activas / Vencidas / Cerradas. */
    private fun pintarChipsEstado() {
        val bandejas = listOf(
            "Todas" to null,
            "Por entregar" to "POR_ENTREGAR",
            "Activas" to "ACTIVAS",
            "Vencidas" to "VENCIDAS",
            "Cerradas" to "CERRADAS",
        )
        val grupo = binding.chipsEstado
        bandejas.forEachIndexed { i, (etiqueta, filtro) ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = etiqueta
                isCheckable = true
                isChecked = i == 0
                setEnsureMinTouchTargetSize(false)
                setOnClickListener { vm.filtrarBandeja(filtro); adapter.refresh() }
            }
            grupo.addView(chip)
        }
    }

    /** Desglose de la renta: sus prendas (foto+nombre+cantidad), depósito, importe y código de retiro. */
    private fun mostrarDesgloseRenta(r: RentaResponse) {
        // Piezas del mismo disfraz → un solo artículo con el nombre del disfraz.
        val (deDisfraz, sueltas) = r.lineas.orEmpty().partition { it.disfrazGrupo != null }
        val disfraces = deDisfraz.groupBy { it.disfrazGrupo!! }.map { (_, piezas) ->
            val primera = piezas.first()
            val porDia = piezas.mapNotNull { it.precioPorDia }
                .fold(java.math.BigDecimal.ZERO) { a, b -> a + b }
            LineaDesglose(
                fotoUrl = piezas.firstOrNull { !it.fotoUrl.isNullOrBlank() }?.fotoUrl,
                nombre = primera.disfrazNombre ?: "Disfraz",
                detalle = "Cantidad: ${primera.disfrazCantidad ?: 1} · ${piezas.size} piezas",
                monto = porDia.comoPrecio()?.let { "$it / dia" },
            )
        }
        val sueltasLd = sueltas.map { l ->
            LineaDesglose(
                fotoUrl = l.fotoUrl,
                nombre = l.nombre ?: "Prenda",
                detalle = "Cantidad: ${l.cantidad ?: 1}",
                monto = l.precioPorDia.comoPrecio()?.let { "$it / dia" },
            )
        }
        val lineas = disfraces + sueltasLd
        val pie = buildList {
            r.deposito?.takeIf { it.signum() > 0 }?.let { add("Deposito" to (it.comoPrecio() ?: "-")) }
            r.importe?.let { add("Importe" to (it.comoPrecio() ?: "-")) }
        }
        mostrarDesglose(
            titulo = "Renta · ${CicloDeRenta.etiqueta(r)}",
            codigoRetiro = r.codigoRetiro,
            subtitulo = listOfNotNull(
                periodoLegible(r.fechaRetiro, r.fechaDevolucion),
                CicloDeRenta.urgencia(r),
            ).joinToString("\n"),
            lineas = lineas,
            pie = pie,
        )
    }

    /** Guarda el PDF en caché y lo abre con un visor del sistema (FileProvider). */
    private fun abrirPdf(bytes: ByteArray) {
        try {
            val dir = File(requireContext().cacheDir, "documentos").apply { mkdirs() }
            val archivo = File(dir, "contrato.pdf")
            archivo.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.fileprovider", archivo,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            mostrarMensaje("No hay una app para ver PDF instalada.")
        } catch (_: Exception) {
            mostrarMensaje("No se pudo abrir el contrato.")
        }
    }

    private fun ejecutar(renta: RentaResponse, accion: AccionRenta) {
        val id = renta.id ?: return
        when (accion) {
            AccionRenta.ENTREGAR -> confirmarEntregar(renta)
            AccionRenta.DEVOLVER -> confirmarDevolverSinRevisar(id)
            AccionRenta.DEVOLVER_DETALLE -> abrirDevolucion(renta)
            AccionRenta.COBRAR -> abrirCobros(renta)
            AccionRenta.CERRAR -> vm.cerrar(id)
            AccionRenta.CANCELAR -> confirmarCancelar(id)
            AccionRenta.EXTENDER -> dialogoExtender(id, renta.fechaDevolucion)
            AccionRenta.CONTRATO -> vm.descargarContrato(id)
        }
    }

    /** Abre la devolucion detallada con una fila por unidad de la renta (lineas o prenda unica). */
    private fun abrirDevolucion(renta: RentaResponse) {
        val id = renta.id ?: return
        val prendaIds = mutableListOf<java.util.UUID>()
        val lineas = renta.lineas
        if (!lineas.isNullOrEmpty()) {
            lineas.forEach { linea ->
                val prendaId = linea.prendaId ?: return@forEach
                repeat(linea.cantidad ?: 1) { prendaIds.add(prendaId) }
            }
        } else {
            renta.prendaId?.let { prendaIds.add(it) }
        }
        if (prendaIds.isEmpty()) {
            mostrarMensaje("La renta no tiene prendas para revisar.")
            return
        }
        findNavController().navigate(
            R.id.devolucionFormFragment,
            DevolucionFormFragment.args(id, renta.deposito, renta.fechaDevolucion, prendaIds),
        )
    }

    private fun abrirCobros(renta: RentaResponse) {
        val id = renta.id ?: return
        findNavController().navigate(
            R.id.pagoConceptoFragment,
            PagoConceptoFragment.args(
                tipo = "RENTA",
                conceptoId = id,
                sucursalId = renta.sucursalId,
                total = renta.importe,
                titulo = "Renta · ${renta.importe.comoPrecio() ?: "-"}",
            ),
        )
    }

    /**
     * Entregar es el momento en que el codigo de retiro importa: se coteja contra el que muestra el
     * cliente. Por eso la confirmacion lo repite, junto con el nombre y cuantos articulos salen.
     */
    private fun confirmarEntregar(renta: RentaResponse) {
        val id = renta.id ?: return
        val articulos = renta.lineas?.sumOf { it.cantidad ?: 0 } ?: 0
        val detalle = buildString {
            append(renta.clienteNombre?.takeIf { it.isNotBlank() } ?: "Cliente sin ficha")
            renta.codigoRetiro?.takeIf { it.isNotBlank() }?.let { append("\nCodigo de retiro: ").append(it) }
            if (articulos > 0) append("\nArticulos que salen: ").append(articulos)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Entregar la renta?")
            .setMessage(detalle)
            .setPositiveButton("Entregar") { _, _ -> vm.entregar(id) }
            .setNegativeButton("Volver", null)
            .show()
    }

    /** La devolucion rapida no revisa piezas: no calcula danos ni faltantes, asi que se avisa. */
    private fun confirmarDevolverSinRevisar(id: java.util.UUID) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Devolver sin revisar?")
            .setMessage("Se marca como devuelta sin revisar pieza por pieza: no se cobran danos ni "
                + "piezas faltantes. Usa 'Registrar devolucion' si quieres revisarla.")
            .setPositiveButton("Devolver igual") { _, _ -> vm.devolver(id) }
            .setNegativeButton("Volver", null)
            .show()
    }

    private fun confirmarCancelar(id: java.util.UUID) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancelar renta?")
            .setMessage("La renta quedara cancelada.")
            .setPositiveButton("Cancelar renta") { _, _ -> vm.cancelar(id) }
            .setNegativeButton("Volver", null)
            .show()
    }

    /**
     * Nueva fecha de devolucion con calendario (antes habia que escribirla a mano en formato ISO y un
     * dedazo daba "Fecha invalida"). Se abre en la fecha actual de la renta y no deja elegir el pasado.
     */
    private fun dialogoExtender(id: java.util.UUID, actual: LocalDate?) {
        val desde = (actual ?: LocalDate.now()).enMillisUtc()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Nueva fecha de devolucion")
            .setSelection(desde)
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build(),
            )
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val fecha = aLocalDate(millis)
            when {
                fecha == null -> mostrarMensaje("No se pudo leer la fecha.")
                actual != null && !fecha.isAfter(actual) ->
                    mostrarMensaje("La nueva fecha debe ser posterior al ${actual.comoDiaMes()}.")
                else -> vm.extender(id, fecha)
            }
        }
        picker.show(childFragmentManager, "extender")
    }

    private fun pintarEstado(refresh: LoadState) {
        when (refresh) {
            is LoadState.Loading -> if (adapter.itemCount == 0) binding.stateView.cargando()
            is LoadState.Error ->
                binding.stateView.error(refresh.error.localizedMessage ?: "No se pudieron cargar las rentas.") {
                    adapter.refresh()
                }
            is LoadState.NotLoading ->
                if (adapter.itemCount == 0) {
                    binding.stateView.vacio("No hay rentas. Registra la primera con el boton +.")
                } else {
                    binding.stateView.ocultar()
                }
        }
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_CLIENTE_ID = "clienteId"
        const val ARG_CLIENTE_NOMBRE = "clienteNombre"

        /** Args para abrir Rentas acotado a un cliente (atajo desde su ficha). */
        fun argsDeCliente(clienteId: java.util.UUID, nombre: String?) = androidx.core.os.bundleOf(
            ARG_CLIENTE_ID to clienteId.toString(),
            ARG_CLIENTE_NOMBRE to nombre,
        )
    }
}
