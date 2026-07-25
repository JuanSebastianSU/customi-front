package com.costumi.app.ui.common

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.costumi.app.databinding.WidgetPeriodoBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.LocalDate

/**
 * Enlaza el selector de periodo (`widget_periodo`). Unico en la app: el cliente y la tienda eligen las
 * fechas de renta de la misma forma —calendario de rango, sin pasado— y ven lo mismo debajo
 * ("01 sep → 04 sep · 3 dias"), en vez de escribir AAAA-MM-DD a mano en el modo gestion.
 */
class SelectorDePeriodo(
    private val fragment: Fragment,
    private val binding: WidgetPeriodoBinding,
    private val titulo: String = "Fechas de renta",
    private val alElegir: (LocalDate, LocalDate) -> Unit = { _, _ -> },
) {
    var retiro: LocalDate? = null
        private set
    var devolucion: LocalDate? = null
        private set

    val completo get() = retiro != null && devolucion != null

    init {
        binding.boton.setOnClickListener { abrir() }
        pintar()
    }

    /** Fija el periodo sin abrir el calendario (al volver a la pantalla, o con un valor por defecto). */
    fun fijar(desde: LocalDate?, hasta: LocalDate?) {
        retiro = desde
        devolucion = hasta
        pintar()
    }

    private fun abrir() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(titulo)
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build(),
            )
            .apply {
                val desde = retiro
                val hasta = devolucion
                if (desde != null && hasta != null) {
                    setSelection(androidx.core.util.Pair(desde.enMillisUtc(), hasta.enMillisUtc()))
                }
            }
            .build()
        picker.addOnPositiveButtonClickListener { rango ->
            retiro = aLocalDate(rango.first)
            devolucion = aLocalDate(rango.second)
            pintar()
            val desde = retiro
            val hasta = devolucion
            if (desde != null && hasta != null) alElegir(desde, hasta)
        }
        picker.show(fragment.childFragmentManager, "periodo")
    }

    private fun pintar() {
        val legible = periodoLegible(retiro, devolucion)
        binding.texto.isVisible = legible != null
        binding.texto.text = legible.orEmpty()
        binding.boton.text = if (legible == null) "Elegir fechas de renta" else "Cambiar fechas"
    }
}
