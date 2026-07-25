package com.costumi.app.ui.gestion.devoluciones

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.FragmentDevolucionFormBinding
import com.costumi.app.databinding.ItemPiezaRevisadaBinding
import com.costumi.app.ui.alEscribir
import com.costumi.app.ui.cargarFoto
import com.costumi.app.ui.common.Tono
import com.costumi.app.ui.common.aLocalDate
import com.costumi.app.ui.common.colorDeTexto
import com.costumi.app.ui.common.comoDiaMes
import com.costumi.app.ui.common.enMillisUtc
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.PiezaRequest
import com.costumi.apiclient.models.RegistrarDevolucionRequest
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Devolucion detallada de una renta: se revisa pieza por pieza y la liquidacion del deposito se ve
 * mientras se revisa (RF-5). Antes el resultado (cuanto se devuelve o se cobra) aparecia recien
 * DESPUES de confirmar, en un aviso.
 */
@AndroidEntryPoint
class DevolucionFormFragment : Fragment(R.layout.fragment_devolucion_form) {

    private val vm: DevolucionFormViewModel by viewModels()
    private var _binding: FragmentDevolucionFormBinding? = null
    private val binding get() = _binding!!

    private class FilaPieza(
        val binding: ItemPiezaRevisadaBinding,
        val prendaId: String,
        val valorDano: BigDecimal?,
        var estado: PiezaRequest.Estado,
    ) {
        /** No volvio: se decide entre cobrar la reposicion o dejar la renta pendiente. */
        val faltante get() = estado == PiezaRequest.Estado.PERDIDA
        val danada get() = estado == PiezaRequest.Estado.DANADA
        val resuelta get() = !faltante || binding.checkPerdidaCobrada.isChecked
    }

    private val filas = mutableListOf<FilaPieza>()
    private var construido = false
    private var fechaReal: LocalDate = LocalDate.now()

    /** El usuario escribio el cargo a mano: desde ahi no se vuelve a sugerir solo. */
    private var danosManual = false
    private var retrasoManual = false

    /** Mientras la pantalla escribe los sugeridos, sus propios listeners no cuentan como "a mano". */
    private var pintando = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDevolucionFormBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.botonConfirmar.setOnClickListener { confirmar() }
        binding.botonFecha.setOnClickListener { elegirFecha() }

        vm.depositoInicial?.let { binding.editDeposito.setText(it) }
        binding.editDeposito.alEscribir { recalcular() }
        binding.editDanos.alEscribir { if (!pintando) { danosManual = true; recalcular() } }
        binding.editRetraso.alEscribir { if (!pintando) { retrasoManual = true; recalcular() } }

        observar(vm.prendas) { estado ->
            binding.stateView.mostrar(estado, vacio = "No hay piezas para revisar.") { prendas ->
                if (!construido) {
                    construido = true
                    construirChecklist(prendas)
                    pintarFecha()
                }
            }
        }
        observar(vm.registrando) { registrando ->
            binding.botonConfirmar.isEnabled = !registrando
            binding.botonConfirmar.text = if (registrando) "Registrando..." else "Registrar devolucion"
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoDev.Registrada -> {
                    setFragmentResult(RESULT_REGISTRADA, Bundle.EMPTY)
                    mostrarMensaje(
                        "Devolucion registrada. Remanente: ${evento.remanente.comoPrecio() ?: "$0"}" +
                            (evento.multa?.takeIf { it.signum() > 0 }?.let { " · multa ${it.comoPrecio()}" } ?: ""),
                    )
                    findNavController().popBackStack()
                }
                is EventoDev.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun construirChecklist(prendas: Map<String, PrendaARevisar>) {
        val total = vm.prendaIds.size
        binding.tituloPiezas.text = if (total == 1) "Revisa la pieza" else "Revisa las $total piezas"
        val contadorPorPrenda = mutableMapOf<String, Int>()
        val unidadesPorPrenda = vm.prendaIds.groupingBy { it }.eachCount()

        vm.prendaIds.forEach { prendaId ->
            val fila = ItemPiezaRevisadaBinding.inflate(layoutInflater, binding.contenedorPiezas, false)
            val prenda = prendas[prendaId]
            val n = (contadorPorPrenda[prendaId] ?: 0) + 1
            contadorPorPrenda[prendaId] = n

            fila.foto.cargarFoto(prenda?.fotoUrl)
            fila.nombrePrenda.text = prenda?.nombre?.takeIf { it.isNotBlank() } ?: "Prenda"
            val unidades = unidadesPorPrenda[prendaId] ?: 1
            fila.unidad.text = listOfNotNull(
                if (unidades > 1) "Unidad $n de $unidades" else null,
                prenda?.valorDano?.takeIf { it.signum() > 0 }?.let { "dano ${it.comoPrecio()}" },
            ).joinToString("  ·  ")
            fila.unidad.isVisible = fila.unidad.text.isNotBlank()
            fila.editDescripcion.setText("${prenda?.nombre.orEmpty().ifBlank { "Prenda" }} #$n")

            val filaPieza = FilaPieza(fila, prendaId, prenda?.valorDano, ESTADOS.first().second)
            for ((etiqueta, estado) in ESTADOS) {
                val chip = Chip(requireContext()).apply {
                    text = etiqueta
                    isCheckable = true
                    isChecked = estado == filaPieza.estado
                    setOnClickListener {
                        filaPieza.estado = estado
                        actualizarFila(filaPieza)
                        recalcular()
                    }
                }
                fila.grupoEstado.addView(chip)
            }
            fila.checkPerdidaCobrada.setOnCheckedChangeListener { _, _ -> actualizarFila(filaPieza); recalcular() }
            fila.botonNota.setOnClickListener {
                fila.tilDescripcion.isVisible = !fila.tilDescripcion.isVisible
                fila.botonNota.text = if (fila.tilDescripcion.isVisible) "Ocultar nota" else "Agregar nota"
            }

            filas.add(filaPieza)
            binding.contenedorPiezas.addView(fila.root)
            actualizarFila(filaPieza)
        }
        recalcular()
    }

    private fun actualizarFila(f: FilaPieza) {
        f.binding.checkPerdidaCobrada.isVisible = f.faltante
        if (!f.faltante) f.binding.checkPerdidaCobrada.isChecked = false
        f.binding.avisoPendiente.isVisible = f.faltante && !f.binding.checkPerdidaCobrada.isChecked
    }

    private fun elegirFecha() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Cuando volvio la prenda")
            .setSelection(fechaReal.enMillisUtc())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            fechaReal = aLocalDate(millis) ?: fechaReal
            pintarFecha()
            recalcular()
        }
        picker.show(childFragmentManager, "fechaReal")
    }

    private fun pintarFecha() {
        binding.fechaPactada.text = vm.fechaDevolucionPactada
            ?.let { "Pactada: ${it.comoDiaMes()}" } ?: "Sin fecha pactada"
        val hoy = fechaReal == LocalDate.now()
        binding.fechaReal.text =
            if (hoy) "Devuelta hoy, ${fechaReal.comoDiaMes()}" else "Devuelta el ${fechaReal.comoDiaMes()}"

        val dias = vm.diasDeRetraso(fechaReal)
        binding.avisoRetraso.isVisible = dias > 0
        binding.avisoRetraso.text = if (dias == 1L) "1 dia de retraso" else "$dias dias de retraso"
        binding.avisoRetraso.setTextColor(Tono.ERROR.colorDeTexto(binding.root))
    }

    /**
     * Rehace la cuenta con lo marcado hasta ahora. Los cargos se SUGIEREN solos (danos de las piezas
     * marcadas, recargo segun la politica de la tienda) hasta que el usuario escriba los suyos: asi el
     * caso normal no exige escribir nada y el excepcional sigue siendo posible.
     */
    private fun recalcular() {
        filas.forEach { f ->
            f.binding.avisoPendiente.isVisible = f.faltante && !f.binding.checkPerdidaCobrada.isChecked
        }

        pintando = true
        if (!danosManual) {
            val sugerido = filas
                .filter { it.danada || (it.faltante && it.binding.checkPerdidaCobrada.isChecked) }
                .mapNotNull { it.valorDano }
                .fold(BigDecimal.ZERO) { acc, v -> acc + v }
            binding.editDanos.setTextSinDisparar(if (sugerido.signum() > 0) sugerido.toPlainString() else "")
        }
        if (!retrasoManual) {
            val sugerido = vm.recargoPorRetraso(fechaReal)
            binding.editRetraso.setTextSinDisparar(if (sugerido.signum() > 0) sugerido.toPlainString() else "")
        }
        pintando = false

        val deposito = leerMonto(binding.editDeposito) ?: BigDecimal.ZERO
        val danos = leerMonto(binding.editDanos) ?: BigDecimal.ZERO
        val retraso = leerMonto(binding.editRetraso) ?: BigDecimal.ZERO
        val liquidacion = Liquidacion(
            deposito = deposito,
            danos = danos,
            retraso = retraso,
            diasRetraso = vm.diasDeRetraso(fechaReal),
            retrasoManual = retrasoManual,
            multasApagadas = !vm.multasActivas,
        )

        binding.avisoMultasApagadas.isVisible = liquidacion.multasApagadas
        binding.tilDanos.isEnabled = !liquidacion.multasApagadas
        binding.tilRetraso.isEnabled = !liquidacion.multasApagadas
        binding.tilRetraso.helperText = when {
            liquidacion.diasRetraso <= 0 -> "Volvio en fecha: sin recargo"
            retrasoManual -> "Lo pusiste a mano"
            // Sin recargo configurado no hay nada que sugerir: se dice, en vez de dejar el campo vacio
            // bajo un texto que promete una sugerencia que no va a llegar.
            !vm.tieneRecargoConfigurado -> "La tienda no cobra recargo por dia (Configuracion)"
            else -> "Sugerido por la politica de la tienda"
        }
        binding.tilDanos.helperText = if (danosManual) "Lo pusiste a mano" else "Sumado de las piezas marcadas"

        binding.desglose.text = buildList {
            add("Deposito ${deposito.comoPrecio() ?: "$0"}")
            if (!liquidacion.multasApagadas) {
                if (danos.signum() > 0) add("danos ${danos.comoPrecio()}")
                if (retraso.signum() > 0) add("retraso ${retraso.comoPrecio()}")
            }
        }.joinToString("  −  ")

        val saldo = liquidacion.saldo
        val aFavorDelCliente = saldo.signum() >= 0
        binding.etiquetaSaldo.text = if (aFavorDelCliente) "A devolver al cliente" else "El cliente queda debiendo"
        binding.valorSaldo.text = saldo.abs().comoPrecio() ?: "$0"
        val tono = if (aFavorDelCliente) Tono.EXITO else Tono.ERROR
        binding.valorSaldo.setTextColor(tono.colorDeTexto(binding.root))
        binding.etiquetaSaldo.setTextColor(tono.colorDeTexto(binding.root))

        val total = filas.size
        val resueltas = filas.count { it.resuelta }
        binding.estadoPiezas.text = when {
            total == 0 -> ""
            resueltas >= total -> "$resueltas de $total piezas resueltas: la renta se cerrara"
            else -> "$resueltas de $total piezas resueltas: quedaran ${total - resueltas} pendientes"
        }
    }

    /** Escribe el sugerido sin mover el cursor si ya decia lo mismo. */
    private fun EditText.setTextSinDisparar(texto: String) {
        if (text?.toString() != texto) setText(texto)
    }

    private fun confirmar() {
        val rentaId = runCatching { UUID.fromString(requireArguments().getString(ARG_RENTA_ID)) }
            .getOrNull() ?: run { mostrarMensaje("Renta invalida"); return }
        if (filas.isEmpty()) { mostrarMensaje("No hay piezas para registrar"); return }

        val piezas = filas.mapIndexed { i, f ->
            val nota = f.binding.editDescripcion.text?.toString()?.trim()
                ?.ifBlank { null } ?: "Pieza ${i + 1}"
            PiezaRequest(
                prendaId = UUID.fromString(f.prendaId),
                estado = f.estado,
                descripcion = nota,
                // Una pieza que no volvio es justamente la que no llego; antes eran dos controles que
                // podian contradecirse (estado "Perdida" con "Llego" marcado).
                llego = !f.faltante,
                perdidaCobrada = f.binding.checkPerdidaCobrada.isChecked,
            )
        }

        vm.registrar(
            RegistrarDevolucionRequest(
                rentaId = rentaId,
                deposito = leerMonto(binding.editDeposito),
                cargoPorDanos = leerMonto(binding.editDanos),
                cargoPorRetraso = leerMonto(binding.editRetraso),
                fechaDevolucionReal = fechaReal,
                piezas = piezas,
            ),
        )
    }

    private fun leerMonto(campo: EditText): BigDecimal? =
        campo.text?.toString()?.trim()?.replace(",", ".")?.toBigDecimalOrNull()

    override fun onDestroyView() {
        filas.clear()
        construido = false
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_REGISTRADA = "devolucion_registrada"
        const val ARG_RENTA_ID = "rentaId"
        const val ARG_DEPOSITO = "deposito"
        const val ARG_FECHA_DEV = "fechaDevolucion"
        const val ARG_PRENDA_IDS = "prendaIds"

        private val ESTADOS = listOf(
            "Bien" to PiezaRequest.Estado.BIEN,
            "Danada" to PiezaRequest.Estado.DANADA,
            "En limpieza" to PiezaRequest.Estado.EN_LIMPIEZA,
            "No llego" to PiezaRequest.Estado.PERDIDA,
        )

        fun args(rentaId: UUID, deposito: BigDecimal?, fechaDevolucion: LocalDate?, prendaIds: List<UUID>) =
            bundleOf(
                ARG_RENTA_ID to rentaId.toString(),
                ARG_DEPOSITO to deposito?.toPlainString(),
                ARG_FECHA_DEV to fechaDevolucion?.toString(),
                ARG_PRENDA_IDS to ArrayList(prendaIds.map { it.toString() }),
            )
    }
}
