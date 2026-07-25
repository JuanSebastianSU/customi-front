package com.costumi.app.ui.gestion.caja

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemTurnoBinding
import com.costumi.app.ui.common.Tono
import com.costumi.app.ui.common.colorDeTexto
import com.costumi.app.ui.common.pintarPastilla
import com.costumi.apiclient.models.TurnoResponse
import java.util.UUID

/** Turnos de caja: estado, sucursal, fondo inicial, movimientos y (si cerrado) diferencia del arqueo. */
class TurnoAdapter(
    private val nombreSucursal: (UUID?) -> String,
    private val alElegir: (TurnoResponse) -> Unit,
) : ListAdapter<TurnoResponse, TurnoAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTurnoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.enlazar(getItem(position))

    inner class VH(private val binding: ItemTurnoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(t: TurnoResponse) {
            val abierto = t.estado == "ABIERTO"
            binding.titulo.text = nombreSucursal(t.sucursalId)
            binding.estado.pintarPastilla(if (abierto) "Abierto" else "Cerrado", if (abierto) Tono.EXITO else Tono.NEUTRO)

            val movs = t.movimientos?.size ?: 0
            binding.detalle.text = "Fondo ${t.fondoInicial.comoPrecio() ?: "$0"}  ·  " +
                if (movs == 1) "1 movimiento" else "$movs movimientos"

            // El resultado del cuadre, en el color que le corresponde: verde OK, rojo falta, ámbar sobra.
            val (texto, tono) = when {
                abierto -> "En curso" to Tono.INFO
                t.diferenciaEfectivo == null -> "Cerrado" to Tono.NEUTRO
                t.diferenciaEfectivo!!.signum() == 0 -> "Cuadre OK" to Tono.EXITO
                t.diferenciaEfectivo!!.signum() > 0 -> "Sobra ${t.diferenciaEfectivo!!.comoPrecio()}" to Tono.ALERTA
                else -> "Falta ${t.diferenciaEfectivo!!.abs().comoPrecio()}" to Tono.ERROR
            }
            binding.monto.text = texto
            binding.monto.setTextColor(tono.colorDeTexto(binding.root))

            binding.root.setOnClickListener { alElegir(t) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TurnoResponse>() {
            override fun areItemsTheSame(a: TurnoResponse, b: TurnoResponse) = a.id == b.id
            override fun areContentsTheSame(a: TurnoResponse, b: TurnoResponse) = a == b
        }
    }
}
