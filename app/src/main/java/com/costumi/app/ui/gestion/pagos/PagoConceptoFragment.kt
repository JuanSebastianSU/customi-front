package com.costumi.app.ui.gestion.pagos

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.DialogCobroBinding
import com.costumi.app.databinding.FragmentPagoConceptoBinding
import com.costumi.app.ui.gestion.DesgloseLineaAdapter
import com.costumi.app.ui.gestion.LineaDesglose
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import androidx.core.view.isVisible
import com.costumi.apiclient.models.ComprobanteResponse
import com.costumi.apiclient.models.RegistrarPagoRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.math.BigDecimal

/** Cobros de una operación (venta/renta): saldo, movimientos, cobrar (simple/mixto) y comprobante PDF. */
@AndroidEntryPoint
class PagoConceptoFragment : Fragment(R.layout.fragment_pago_concepto) {

    private val vm: PagoConceptoViewModel by viewModels()
    private var _binding: FragmentPagoConceptoBinding? = null
    private val binding get() = _binding!!
    private val adapter = PagoAdapter()
    private val articulosAdapter = DesgloseLineaAdapter()
    private var pendienteActual: BigDecimal = BigDecimal.ZERO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPagoConceptoBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter
        binding.listaArticulos.adapter = articulosAdapter
        binding.tituloOperacion.text = vm.titulo
        binding.botonCobrar.setOnClickListener { dialogoCobro() }
        binding.botonEnLinea.setOnClickListener { confirmarEnLinea() }
        binding.botonComprobante.setOnClickListener { vm.descargarComprobante() }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "Sin movimientos todavia.") { comp ->
                pintarResumen(comp)
                adapter.submitList(comp.pagos.orEmpty())
            }
        }
        observar(vm.articulos) { arts ->
            binding.tituloArticulos.isVisible = arts.isNotEmpty()
            binding.listaArticulos.isVisible = arts.isNotEmpty()
            articulosAdapter.submitList(arts.map { a ->
                val precio = a.precio.comoPrecio()
                val detalle = buildString {
                    append("Cantidad: ${a.cantidad}")
                    if (precio != null) append("  ·  $precio${if (a.porDia) "/dia" else " c/u"}")
                }
                LineaDesglose(fotoUrl = a.fotoUrl, nombre = a.nombre, detalle = detalle, monto = a.subtotal.comoPrecio())
            })
        }
        observar(vm.procesando) { p -> binding.botonCobrar.isEnabled = !p }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoPago.Registrado -> { mostrarMensaje(evento.mensaje); setFragmentResult(RESULT_COBRADO, Bundle.EMPTY) }
                is EventoPago.Comprobante -> abrirPdf(evento.bytes)
                is EventoPago.Checkout -> abrirCheckout(evento.url)
                is EventoPago.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun pintarResumen(comp: ComprobanteResponse) {
        pendienteActual = vm.pendiente(comp)
        binding.pendiente.text = "Pendiente: ${pendienteActual.comoPrecio() ?: "$0"}"
        val partes = mutableListOf("Cobrado ${comp.totalCobrado.comoPrecio() ?: "$0"}")
        comp.multa?.takeIf { it.signum() > 0 }?.let { partes.add("multa ${it.comoPrecio()}") }
        comp.impuesto?.takeIf { it.signum() > 0 }?.let { imp ->
            val extra = listOfNotNull(
                comp.baseImponible?.comoPrecio()?.let { "base $it" },
                comp.tasaImpuesto?.takeIf { it.signum() > 0 }?.let { "${it.multiply(BigDecimal(100)).toInt()}%" },
            ).joinToString(", ")
            partes.add("impuesto ${imp.comoPrecio()}" + if (extra.isNotBlank()) " ($extra)" else "")
        }
        comp.totalReembolsado?.takeIf { it.signum() > 0 }?.let { partes.add("reembolsado ${it.comoPrecio()}") }
        comp.deposito?.activo?.takeIf { it.signum() > 0 }?.let { partes.add("deposito ${it.comoPrecio()}") }
        binding.desglose.text = partes.joinToString(" · ")
    }

    private fun dialogoCobro() {
        val d = DialogCobroBinding.inflate(layoutInflater)
        if (pendienteActual.signum() > 0) d.editMonto.setText(pendienteActual.toPlainString())

        val metodos = listOf(
            "Efectivo" to RegistrarPagoRequest.Metodo.EFECTIVO,
            "Tarjeta" to RegistrarPagoRequest.Metodo.TARJETA,
            "Transferencia" to RegistrarPagoRequest.Metodo.TRANSFERENCIA,
        )
        d.dropMetodo.setSimpleItems(metodos.map { it.first }.toTypedArray())
        d.dropMetodo.setText(metodos.first().first, false)

        val tipos = buildList {
            add("Cobro" to RegistrarPagoRequest.TipoPago.COBRO)
            if (vm.esRenta) {
                add("Deposito" to RegistrarPagoRequest.TipoPago.DEPOSITO)
                add("Devolucion deposito" to RegistrarPagoRequest.TipoPago.DEVOLUCION_DEPOSITO)
            }
            add("Reembolso" to RegistrarPagoRequest.TipoPago.REEMBOLSO)
        }
        d.dropTipoPago.setSimpleItems(tipos.map { it.first }.toTypedArray())
        d.dropTipoPago.setText(tipos.first().first, false)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Registrar pago")
            .setView(d.root)
            .setPositiveButton("Registrar") { _, _ ->
                val monto = d.editMonto.text?.toString()?.trim()?.replace(",", ".")?.toBigDecimalOrNull()
                if (monto == null || monto.signum() <= 0) { mostrarMensaje("Monto invalido"); return@setPositiveButton }
                val metodo = metodos.firstOrNull { it.first == d.dropMetodo.text?.toString() }?.second ?: metodos.first().second
                val tipo = tipos.firstOrNull { it.first == d.dropTipoPago.text?.toString() }?.second ?: tipos.first().second
                vm.cobrar(monto, metodo, tipo, d.editReferencia.text?.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Confirma el monto y crea el intento de pago en línea (checkout alojado, RF-6.11). */
    private fun confirmarEnLinea() {
        if (pendienteActual.signum() <= 0) {
            mostrarMensaje("No hay saldo pendiente para cobrar en linea.")
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pagar en linea")
            .setMessage("Se abrira el checkout de la pasarela por ${pendienteActual.comoPrecio() ?: "$0"}. El pago se confirma solo cuando la pasarela lo notifica.")
            .setPositiveButton("Continuar") { _, _ -> vm.pagarEnLinea(pendienteActual) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirCheckout(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            mostrarMensaje("No hay un navegador para abrir el checkout.")
        }
    }

    private fun abrirPdf(bytes: ByteArray) {
        try {
            val dir = File(requireContext().cacheDir, "documentos").apply { mkdirs() }
            val archivo = File(dir, "comprobante.pdf")
            archivo.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.fileprovider", archivo,
            )
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
            )
        } catch (_: ActivityNotFoundException) {
            mostrarMensaje("No hay una app para ver PDF instalada.")
        } catch (_: Exception) {
            mostrarMensaje("No se pudo abrir el comprobante.")
        }
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_COBRADO = "pago_cobrado"
        const val ARG_TIPO = "tipoConcepto"
        const val ARG_CONCEPTO_ID = "conceptoId"
        const val ARG_SUCURSAL_ID = "sucursalId"
        const val ARG_TOTAL = "total"
        const val ARG_TITULO = "titulo"

        fun args(tipo: String, conceptoId: java.util.UUID, sucursalId: java.util.UUID?, total: BigDecimal?, titulo: String) =
            bundleOf(
                ARG_TIPO to tipo,
                ARG_CONCEPTO_ID to conceptoId.toString(),
                ARG_SUCURSAL_ID to sucursalId?.toString(),
                ARG_TOTAL to total?.toPlainString(),
                ARG_TITULO to titulo,
            )
    }
}
