package com.costumi.app.ui.gestion.dashboard

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.data.repo.ResumenDashboard
import com.costumi.app.databinding.FragmentDashboardBinding
import com.costumi.app.databinding.ItemAlertaBinding
import com.costumi.app.databinding.ItemKpiBinding
import com.costumi.app.ui.common.colorDeTexto
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Panel del modo GESTION. Responde en orden: donde estoy, que requiere atencion hoy (y de un toque
 * voy a resolverlo), como va el negocio y como esta el inventario.
 */
@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val vm: DashboardViewModel by viewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDashboardBinding.bind(view)

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado) { d -> pintar(d) }
        }

        // El nombre de la tienda encabeza el panel: con varias tiendas no habia forma de saber en cual
        // se esta operando.
        observar(vm.tienda) { nombre ->
            binding.toolbar.title = nombre?.takeIf { it.isNotBlank() } ?: "Panel"
        }
        binding.fecha.text = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es")))
            .replaceFirstChar { it.uppercase() }

        observar(vm.alertas) { pintarAlertas(it) }

        binding.fabVenta.setOnClickListener { findNavController().navigate(R.id.ventaPosFragment) }

        // Al volver al panel se recalcula: pudo reponerse stock o cerrarse una renta en otra pantalla.
        vm.cargarAlertas()
    }

    /** Una fila por alerta, en orden de gravedad; si no hay ninguna, se dice explicitamente. */
    private fun pintarAlertas(alertas: List<AlertaPanel>) {
        val contenedor = binding.contenedorAlertas
        contenedor.removeAllViews()
        for (alerta in alertas) {
            val fila = ItemAlertaBinding.inflate(layoutInflater, contenedor, false)
            fila.titulo.text = alerta.titulo
            fila.detalle.text = alerta.detalle
            fila.franja.setBackgroundColor(alerta.tono.colorDeTexto(fila.root))
            fila.root.setOnClickListener { findNavController().navigate(alerta.destino) }
            contenedor.addView(fila.root)
        }
        contenedor.isVisible = alertas.isNotEmpty()
        binding.sinAlertas.isVisible = alertas.isEmpty()
        binding.tituloAtencion.text = if (alertas.isEmpty()) "Estado de hoy" else "Requiere atencion"
    }

    private fun pintar(d: ResumenDashboard) {
        binding.valorIngresoTotal.text = d.ingresoTotal.comoPrecio() ?: "$0"
        binding.valorIngresoRenta.text = d.ingresoRenta.comoPrecio() ?: "$0"
        binding.valorIngresoVenta.text = d.ingresoVenta.comoPrecio() ?: "$0"

        kpi(binding.kpiDisponibles, entero(d.disponibles), "Disponibles para alquilar")
        kpi(binding.kpiRentadas, entero(d.rentadasAhora), "Rentadas ahora")
        kpi(binding.kpiValorInv, d.valorInventario.comoPrecio() ?: "$0", "Valor inventario")
        kpi(binding.kpiUtilizacion, porcentaje(d.tasaUtilizacion), "Utilizacion")

        // Lo que esta fuera de circulacion casi siempre es cero: se nombra solo lo que no lo es, en vez
        // de gastar cuatro tarjetas (y dos renglones) en repetir ceros.
        val fuera = listOfNotNull(
            (d.enLimpieza ?: 0L).takeIf { it > 0 }?.let { "$it en limpieza" },
            (d.danadas ?: 0L).takeIf { it > 0 }?.let { "$it danadas" },
            (d.perdidas ?: 0L).takeIf { it > 0 }?.let { "$it perdidas" },
        )
        binding.fueraDeCirculacion.text = if (fuera.isEmpty()) {
            "${entero(d.totalUnidades)} unidades en total, ninguna fuera de circulacion."
        } else {
            "${entero(d.totalUnidades)} unidades en total  ·  fuera de circulacion: ${fuera.joinToString(", ")}."
        }
    }

    private fun kpi(item: ItemKpiBinding, valor: String, etiqueta: String) {
        item.kpiNumero.text = valor
        item.kpiEtiqueta.text = etiqueta
    }

    private fun entero(v: Long?): String = (v ?: 0L).toString()

    private fun porcentaje(v: BigDecimal?): String {
        val pct = (v ?: BigDecimal.ZERO).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)
        return "$pct%"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
