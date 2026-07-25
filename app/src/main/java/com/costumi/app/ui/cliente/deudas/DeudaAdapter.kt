package com.costumi.app.ui.cliente.deudas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.data.remote.MiDeudaDto
import com.costumi.app.databinding.ItemDeudaBinding
import com.costumi.app.ui.common.Tono
import com.costumi.app.ui.common.pintarPastilla
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/** Una multa/saldo por renta, con el desglose que explica de donde sale el cargo. */
class DeudaAdapter : ListAdapter<MiDeudaDto, DeudaAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDeudaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemDeudaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(d: MiDeudaDto) {
            binding.tienda.text = d.empresaNombre ?: "Tienda"

            // Se destaca lo que TODAVIA debe. Si ya lo pago, la multa sigue visible en el desglose pero
            // sin cifra en rojo: verla en rojo despues de pagar seria alarmante y falso.
            val saldo = d.saldo ?: BigDecimal.ZERO
            val pendiente = saldo.signum() > 0
            binding.saldo.isVisible = pendiente
            binding.saldo.text = saldo.comoPrecio()

            // La pastilla dice el estado sin depender del rojo del importe (que se oculta al pagar).
            if (pendiente) binding.chipEstado.pintarPastilla("Pendiente", Tono.ERROR)
            else binding.chipEstado.pintarPastilla("Pagado", Tono.NEUTRO)

            val fechas = if (d.fechaRetiro != null && d.fechaDevolucion != null) {
                "${d.fechaRetiro.format(FORMATO)} - ${d.fechaDevolucion.format(FORMATO)}"
            } else {
                null
            }
            binding.referencia.text = listOfNotNull(d.codigoRetiro, fechas).joinToString("  ·  ")

            binding.desglose.text = desgloseDe(d)
        }

        /** "Danos $150 + Retraso $0 - Deposito $50 = Multa $100", omitiendo lo que este en cero. */
        private fun desgloseDe(d: MiDeudaDto): String {
            val partes = mutableListOf<String>()
            d.cargoPorDanos?.takeIf { it.signum() > 0 }?.let { partes.add("Danos ${it.comoPrecio()}") }
            d.cargoPorRetraso?.takeIf { it.signum() > 0 }?.let { partes.add("Retraso ${it.comoPrecio()}") }
            val cargos = partes.joinToString(" + ")
            val deposito = d.deposito?.takeIf { it.signum() > 0 }?.let { " - Deposito ${it.comoPrecio()}" }.orEmpty()
            val multa = d.multa ?: BigDecimal.ZERO
            return when {
                // Sin cargos, lo que debe es la renta misma (todavia no la termino de pagar).
                cargos.isEmpty() -> "Renta ${d.importe.comoPrecio()}  ·  pagado ${d.pagado.comoPrecio()}"
                multa.signum() > 0 -> "$cargos$deposito = Multa ${multa.comoPrecio()}"
                // Los cargos no superaron el deposito: no hay multa que cobrar.
                else -> "$cargos$deposito  ·  cubierto por el deposito"
            }
        }
    }

    private companion object {
        val FORMATO: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM")

        val DIFF = object : DiffUtil.ItemCallback<MiDeudaDto>() {
            override fun areItemsTheSame(a: MiDeudaDto, b: MiDeudaDto) = a.rentaId == b.rentaId
            override fun areContentsTheSame(a: MiDeudaDto, b: MiDeudaDto) = a == b
        }
    }
}
