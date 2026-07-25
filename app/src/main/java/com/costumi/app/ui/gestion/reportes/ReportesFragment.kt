package com.costumi.app.ui.gestion.reportes

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.FragmentReportesBinding
import com.costumi.app.databinding.ItemReporteFilaBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.app.data.repo.ReporteCompleto
import com.costumi.apiclient.models.ArticuloRanking
import com.costumi.apiclient.models.DisfrazRanking
import com.costumi.apiclient.models.EmpleadoVentas
import com.costumi.apiclient.models.GrupoInventario
import com.costumi.apiclient.models.RentaVencidaResponse
import com.costumi.apiclient.models.SucursalResponse
import com.costumi.apiclient.models.TipoEtiquetaResponse
import com.costumi.apiclient.models.ValorEtiquetaRanking
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/** Tablero de Reportes: ingresos, ganancia, método, depósitos, rankings, vencidas y exportes. */
@AndroidEntryPoint
class ReportesFragment : Fragment(R.layout.fragment_reportes) {

    private val vm: ReportesViewModel by viewModels()
    private var _binding: FragmentReportesBinding? = null
    private val binding get() = _binding!!

    private var sucursales: List<SucursalResponse> = emptyList()
    private var tipos: List<TipoEtiquetaResponse> = emptyList()
    private var filtroSucursal: UUID? = null
    private var filtroDesde: LocalDate? = null
    private var filtroHasta: LocalDate? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentReportesBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.botonInvCsv.setOnClickListener { vm.exportarInventario(pdf = false) }
        binding.botonInvPdf.setOnClickListener { vm.exportarInventario(pdf = true) }
        binding.botonVenCsv.setOnClickListener { vm.exportarRentasVencidas(pdf = false) }
        binding.botonVenPdf.setOnClickListener { vm.exportarRentasVencidas(pdf = true) }
        binding.botonRango.setOnClickListener { elegirRango() }
        binding.botonLimpiarFiltro.setOnClickListener {
            filtroDesde = null; filtroHasta = null
            binding.botonRango.text = "Rango de fechas"
            vm.filtrar(filtroSucursal, null, null)
        }

        observar(vm.sucursales) { lista ->
            sucursales = lista
            binding.dropSucursalFiltro.setSimpleItems((listOf("Todas") + lista.map { it.nombre.orEmpty() }).toTypedArray())
            binding.dropSucursalFiltro.setText("Todas", false)
            binding.dropSucursalFiltro.setOnItemClickListener { _, _, pos, _ ->
                filtroSucursal = if (pos == 0) null else lista.getOrNull(pos - 1)?.id
                vm.filtrar(filtroSucursal, filtroDesde, filtroHasta)
            }
        }
        observar(vm.tipos) { lista ->
            tipos = lista
            binding.dropTipoEtiqueta.setSimpleItems(lista.map { it.nombre.orEmpty() }.toTypedArray())
            binding.dropTipoEtiqueta.setOnItemClickListener { _, _, pos, _ ->
                lista.getOrNull(pos)?.id?.let { vm.cargarPorEtiqueta(it) }
            }
        }
        observar(vm.porEtiqueta) { pintarEtiqueta(it) }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "Sin datos de reportes.") { pintar(it) }
        }
        observar(vm.exportando) { exp ->
            listOf(binding.botonInvCsv, binding.botonInvPdf, binding.botonVenCsv, binding.botonVenPdf)
                .forEach { it.isEnabled = !exp }
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoReporte.Archivo -> abrirArchivo(evento.bytes, evento.nombre, evento.esPdf)
                is EventoReporte.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun pintar(r: ReporteCompleto) {
        binding.ingresoTotal.text = r.ingresos?.total.comoPrecio() ?: "$0"
        binding.ingresoDetalle.text =
            "Renta ${r.ingresos?.ingresosPorRenta.comoPrecio() ?: "$0"} · Venta ${r.ingresos?.ingresosPorVenta.comoPrecio() ?: "$0"}"

        val g = r.ganancia
        binding.ganancia.text =
            "Ingresos ${g?.ingresos.comoPrecio() ?: "$0"} − costo ${g?.costoDeVentas.comoPrecio() ?: "$0"} = ${g?.ganancia.comoPrecio() ?: "$0"}"

        val m = r.porMetodo
        binding.porMetodo.text =
            "Efectivo ${m?.efectivo.comoPrecio() ?: "$0"} · Tarjeta ${m?.tarjeta.comoPrecio() ?: "$0"} · Transf. ${m?.transferencia.comoPrecio() ?: "$0"}"

        binding.depositos.text = "Depositos activos: ${r.depositos?.total.comoPrecio() ?: "$0"}"

        pintarRanking(binding.contenedorVendidos, binding.tituloVendidos, r.masVendidos)
        pintarRanking(binding.contenedorRentados, binding.tituloRentados, r.masRentados)
        pintarRankingDisfraces(binding.contenedorDisfracesVendidos, binding.tituloDisfracesVendidos,
            r.disfracesMasVendidos)
        pintarRankingDisfraces(binding.contenedorDisfracesRentados, binding.tituloDisfracesRentados,
            r.disfracesMasRentados)
        pintarVencidas(r.rentasVencidas)
        pintarEmpleados(r.ventasPorEmpleado)
        pintarTablero(r.tablero)
    }

    private fun pintarEtiqueta(items: List<ValorEtiquetaRanking>) {
        val c = _binding?.contenedorEtiqueta ?: return
        c.removeAllViews()
        if (items.isEmpty()) {
            // "Sin datos" solo tras elegir un tipo (para que el usuario sepa que no hay movimientos, no que esté roto).
            if (!binding.dropTipoEtiqueta.text.isNullOrBlank()) {
                fila(c, "Sin ventas ni rentas de prendas con esta etiqueta.", "")
            }
            return
        }
        items.forEach { v ->
            fila(c, "${v.valor.orEmpty()} (${v.unidades ?: 0} uds)", v.monto.comoPrecio() ?: "-")
        }
    }

    private fun pintarTablero(items: List<GrupoInventario>) {
        binding.contenedorTablero.removeAllViews()
        binding.tituloTablero.isVisible = items.isNotEmpty()
        items.forEach { g ->
            val disp = "Disp ${g.disponibles ?: 0}"
            val extra = buildList {
                (g.rentadas ?: 0).takeIf { it > 0 }?.let { add("Rent $it") }
                (g.danadas ?: 0).takeIf { it > 0 }?.let { add("Dañ $it") }
                (g.enLimpieza ?: 0).takeIf { it > 0 }?.let { add("Limp $it") }
                (g.perdidas ?: 0).takeIf { it > 0 }?.let { add("Perd $it") }
            }.joinToString(" · ")
            val nombre = if (extra.isBlank()) g.prendaNombre.orEmpty() else "${g.prendaNombre.orEmpty()}  ·  $extra"
            fila(binding.contenedorTablero, nombre, disp)
        }
    }

    private fun elegirRango() {
        val picker = MaterialDatePicker.Builder.dateRangePicker().setTitleText("Rango de fechas").build()
        picker.addOnPositiveButtonClickListener { rango ->
            filtroDesde = aLocalDate(rango.first)
            filtroHasta = aLocalDate(rango.second)
            binding.botonRango.text = "${filtroDesde ?: ""} → ${filtroHasta ?: ""}"
            vm.filtrar(filtroSucursal, filtroDesde, filtroHasta)
        }
        picker.show(childFragmentManager, "rango")
    }

    private fun aLocalDate(millis: Long?): LocalDate? =
        millis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }

    private fun pintarRanking(contenedor: ViewGroup, titulo: View, items: List<ArticuloRanking>) {
        contenedor.removeAllViews()
        titulo.isVisible = items.isNotEmpty()
        contenedor.isVisible = items.isNotEmpty()
        val max = items.mapNotNull { it.monto }.maxOrNull()
        items.forEach { a ->
            fila(
                contenedor, "${a.nombre.orEmpty()} (${a.unidades ?: 0} uds)", a.monto.comoPrecio() ?: "-",
                fraccion = fraccionDe(a.monto, max),
            )
        }
    }

    /**
     * Igual que [pintarRanking] pero contando DISFRACES: al cobrar, un disfraz se resuelve a sus piezas,
     * asi que el ranking de prendas nunca responde "que disfraz se vende mas".
     */
    private fun pintarRankingDisfraces(contenedor: ViewGroup, titulo: View, items: List<DisfrazRanking>) {
        contenedor.removeAllViews()
        titulo.isVisible = items.isNotEmpty()
        contenedor.isVisible = items.isNotEmpty()
        val max = items.mapNotNull { it.monto }.maxOrNull()
        items.forEach { d ->
            val unidades = d.unidades ?: 0
            val sufijo = if (unidades == 1L) "disfraz" else "disfraces"
            fila(
                contenedor, "${d.nombre.orEmpty()} ($unidades $sufijo)", d.monto.comoPrecio() ?: "-",
                fraccion = fraccionDe(d.monto, max),
            )
        }
    }

    private fun pintarVencidas(items: List<RentaVencidaResponse>) {
        binding.contenedorVencidas.removeAllViews()
        val hay = items.isNotEmpty()
        binding.tituloVencidas.isVisible = hay
        binding.contenedorVencidas.isVisible = hay
        items.forEach { v ->
            fila(
                binding.contenedorVencidas,
                "${v.diasVencida ?: 0} dias · vence ${v.fechaDevolucion ?: "?"}",
                v.importe.comoPrecio() ?: "-",
            )
        }
    }

    private fun pintarEmpleados(items: List<EmpleadoVentas>) {
        binding.contenedorEmpleados.removeAllViews()
        val hay = items.isNotEmpty()
        binding.tituloEmpleados.isVisible = hay
        binding.contenedorEmpleados.isVisible = hay
        val max = items.mapNotNull { it.total }.maxOrNull()
        items.forEach { e ->
            fila(
                binding.contenedorEmpleados, "${e.email.orEmpty()} (${e.numeroVentas ?: 0})",
                e.total.comoPrecio() ?: "-", fraccion = fraccionDe(e.total, max),
            )
        }
    }

    /** Fracción 0..1 del valor respecto del máximo del ranking, para el ancho de la barra. */
    private fun fraccionDe(valor: java.math.BigDecimal?, max: java.math.BigDecimal?): Float? {
        if (valor == null || max == null || max.signum() <= 0) return null
        return (valor.toDouble() / max.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    /** [fraccion] null = fila sin barra (listas que no son ranking). */
    private fun fila(contenedor: ViewGroup, etiqueta: String, valor: String, fraccion: Float? = null) {
        val f = ItemReporteFilaBinding.inflate(layoutInflater, contenedor, false)
        f.etiqueta.text = etiqueta
        f.valor.text = valor
        f.barra.isVisible = fraccion != null
        if (fraccion != null) {
            // La barra son dos Views con peso: llena = fracción, resto = 1 - fracción. Un mínimo visible
            // para que hasta el más chico se distinga del vacío.
            val llena = fraccion.coerceIn(0.03f, 1f)
            (f.barraLlena.layoutParams as android.widget.LinearLayout.LayoutParams).weight = llena
            (f.barraResto.layoutParams as android.widget.LinearLayout.LayoutParams).weight = 1f - llena
        }
        contenedor.addView(f.root)
    }

    /** PDF → visor del sistema; CSV → hoja de compartir (pocos dispositivos abren CSV directo). */
    private fun abrirArchivo(bytes: ByteArray, nombre: String, esPdf: Boolean) {
        try {
            val dir = File(requireContext().cacheDir, "documentos").apply { mkdirs() }
            val archivo = File(dir, nombre)
            archivo.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.fileprovider", archivo,
            )
            val tipo = if (esPdf) "application/pdf" else "text/csv"
            val intent = if (esPdf) {
                Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, tipo) }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = tipo
                    putExtra(Intent.EXTRA_STREAM, uri)
                }
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(if (esPdf) intent else Intent.createChooser(intent, "Compartir CSV"))
        } catch (_: ActivityNotFoundException) {
            mostrarMensaje(if (esPdf) "No hay una app para ver PDF." else "No hay una app para abrir el CSV.")
        } catch (_: Exception) {
            mostrarMensaje("No se pudo abrir el archivo.")
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
