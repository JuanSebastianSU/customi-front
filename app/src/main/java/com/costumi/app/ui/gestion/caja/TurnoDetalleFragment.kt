package com.costumi.app.ui.gestion.caja

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.DialogCerrarTurnoBinding
import com.costumi.app.databinding.DialogMovimientoBinding
import com.costumi.app.databinding.FragmentTurnoDetalleBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.RegistrarMovimientoRequest
import com.costumi.apiclient.models.TurnoResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.util.UUID

/** Detalle de un turno: corte por método, movimientos y acciones (registrar movimiento, cerrar). */
@AndroidEntryPoint
class TurnoDetalleFragment : Fragment(R.layout.fragment_turno_detalle) {

    private val vm: TurnoDetalleViewModel by viewModels()
    private var _binding: FragmentTurnoDetalleBinding? = null
    private val binding get() = _binding!!
    private val adapter = MovimientoAdapter()
    private var esperadoEfectivo: BigDecimal = BigDecimal.ZERO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTurnoDetalleBinding.bind(view)
        binding.toolbar.title = vm.sucursalNombre
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter
        binding.botonMovimiento.setOnClickListener { dialogoMovimiento() }
        binding.botonCerrar.setOnClickListener { dialogoCerrar() }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "Sin movimientos.") { turno ->
                pintar(turno)
                adapter.submitList(turno.movimientos.orEmpty())
            }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoCaja.Info -> { mostrarMensaje(evento.mensaje); setFragmentResult(RESULT_CAMBIO, Bundle.EMPTY) }
                is EventoCaja.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun pintar(turno: TurnoResponse) {
        binding.tarjetaResumen.isVisible = true
        val abierto = turno.estado == "ABIERTO"
        binding.acciones.isVisible = abierto
        binding.estado.text = "${turno.estado.orEmpty()} · ${vm.sucursalNombre}"
        esperadoEfectivo = vm.efectivoEsperado(turno)

        val lineas = mutableListOf("Efectivo esperado: ${esperadoEfectivo.comoPrecio() ?: "$0"}")
        turno.corte?.get("TARJETA")?.takeIf { it.signum() != 0 }?.let { lineas.add("Tarjeta: ${it.comoPrecio()}") }
        turno.corte?.get("TRANSFERENCIA")?.takeIf { it.signum() != 0 }?.let { lineas.add("Transferencia: ${it.comoPrecio()}") }
        lineas.add("Fondo inicial: ${turno.fondoInicial.comoPrecio() ?: "$0"}")
        binding.corte.text = lineas.joinToString("\n")

        if (!abierto && turno.efectivoContado != null) {
            val dif = turno.diferenciaEfectivo ?: BigDecimal.ZERO
            val nota = when {
                dif.signum() == 0 -> "cuadre OK"
                dif.signum() > 0 -> "sobra ${dif.comoPrecio()}"
                else -> "falta ${dif.abs().comoPrecio()}"
            }
            binding.cuadre.isVisible = true
            binding.cuadre.text = "Contado ${turno.efectivoContado.comoPrecio()} · $nota"
        } else {
            binding.cuadre.isVisible = false
        }
    }

    private fun dialogoMovimiento() {
        val d = DialogMovimientoBinding.inflate(layoutInflater)
        val tipos = listOf(
            "Ingreso" to RegistrarMovimientoRequest.Tipo.INGRESO,
            "Egreso" to RegistrarMovimientoRequest.Tipo.EGRESO,
        )
        d.dropTipo.setSimpleItems(tipos.map { it.first }.toTypedArray())
        d.dropTipo.setText(tipos.first().first, false)
        val metodos = listOf(
            "Efectivo" to RegistrarMovimientoRequest.Metodo.EFECTIVO,
            "Tarjeta" to RegistrarMovimientoRequest.Metodo.TARJETA,
            "Transferencia" to RegistrarMovimientoRequest.Metodo.TRANSFERENCIA,
        )
        d.dropMetodo.setSimpleItems(metodos.map { it.first }.toTypedArray())
        d.dropMetodo.setText(metodos.first().first, false)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Registrar movimiento")
            .setView(d.root)
            .setPositiveButton("Registrar") { _, _ ->
                val monto = d.editMonto.text?.toString()?.trim()?.replace(",", ".")?.toBigDecimalOrNull()
                if (monto == null || monto.signum() <= 0) { mostrarMensaje("Monto invalido"); return@setPositiveButton }
                // El backend exige concepto (NotBlank).
                val concepto = d.editConcepto.text?.toString()?.trim().orEmpty()
                if (concepto.isBlank()) { mostrarMensaje("El concepto es obligatorio"); return@setPositiveButton }
                val tipo = tipos.firstOrNull { it.first == d.dropTipo.text?.toString() }?.second ?: tipos.first().second
                val metodo = metodos.firstOrNull { it.first == d.dropMetodo.text?.toString() }?.second ?: metodos.first().second
                vm.registrarMovimiento(
                    RegistrarMovimientoRequest(tipo = tipo, monto = monto, metodo = metodo, concepto = concepto),
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogoCerrar() {
        val d = DialogCerrarTurnoBinding.inflate(layoutInflater)
        d.esperado.text = "Efectivo esperado: ${esperadoEfectivo.comoPrecio() ?: "$0"}"
        fun recalcular() {
            val contado = d.editContado.text?.toString()?.trim()?.replace(",", ".")?.toBigDecimalOrNull()
            if (contado == null) {
                d.diferencia.text = ""
            } else {
                val dif = contado - esperadoEfectivo
                val nota = when {
                    dif.signum() == 0 -> "cuadre OK"
                    dif.signum() > 0 -> "sobra ${dif.comoPrecio()}"
                    else -> "falta ${dif.abs().comoPrecio()}"
                }
                d.diferencia.text = "Diferencia: ${dif.comoPrecio()} ($nota)"
            }
        }
        d.editContado.doAfterTextChanged { recalcular() }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cerrar turno")
            .setView(d.root)
            .setPositiveButton("Cerrar") { _, _ ->
                val contado = d.editContado.text?.toString()?.trim()?.replace(",", ".")?.toBigDecimalOrNull()
                if (contado == null || contado.signum() < 0) { mostrarMensaje("Efectivo contado invalido"); return@setPositiveButton }
                vm.cerrar(contado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_CAMBIO = "turno_cambio"
        const val ARG_ID = "turnoId"
        const val ARG_SUCURSAL = "sucursalNombre"

        fun args(turnoId: UUID, sucursalNombre: String?) = bundleOf(
            ARG_ID to turnoId.toString(),
            ARG_SUCURSAL to sucursalNombre,
        )
    }
}
