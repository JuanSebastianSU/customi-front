package com.costumi.app.ui.gestion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.core.ModoApp
import com.costumi.app.core.Rol
import com.costumi.app.databinding.FragmentMasBinding
import com.costumi.app.ui.irAHome
import com.costumi.app.ui.irALogin
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.SucursalResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/**
 * "Mas": el resto de secciones de Gestion (Rentas, Reembolsos, Reportes, Empleados, etc.) y el
 * cierre de sesion.
 *
 * Las secciones se **filtran por rol** (H2 de la spec): un empleado de bodega/mostrador NO debe ver
 * Reportes, Empleados, Sucursales, Configuracion, Mensajes ni Auditoria (son de administracion y le
 * darian 403). Lo que un rol no puede hacer, no se muestra. El filtro fino ideal es la matriz de
 * permisos del propio empleado; hoy no hay endpoint "mis permisos" -> se anota en el lote de backend.
 */
@AndroidEntryPoint
class MasFragment : Fragment(R.layout.fragment_mas) {

    private val vm: GestionShellViewModel by viewModels()
    private var _binding: FragmentMasBinding? = null
    private val binding get() = _binding!!

    private var rolActual: Rol? = null
    private var seccionesPermitidas: Set<String>? = null
    private var sucursalesActuales: List<SucursalResponse> = emptyList()

    private val secciones = listOf(
        "Rentas",
        "Devoluciones",
        "Pagos y cobros",
        "Caja / turnos",
        "Reembolsos",
        "Reportes",
        "Empleados",
        "Sucursales",
        "Identidad de la tienda",
        "Configuracion",
        "Notificaciones",
        "Mensajes automaticos",
        "Auditoria",
    )

    /** Cada sección de «Más» → la {@code Seccion} del backend que la habilita (para el filtro por permisos). */
    private val seccionEnumDe = mapOf(
        "Rentas" to "RENTAS",
        "Devoluciones" to "DEVOLUCIONES",
        "Pagos y cobros" to "PAGOS",
        "Caja / turnos" to "CAJA",
        "Reembolsos" to "REEMBOLSOS",
        "Reportes" to "REPORTES",
        "Empleados" to "EMPLEADOS",
        "Sucursales" to "SUCURSALES",
        "Identidad de la tienda" to "IDENTIDAD_TIENDA",
        "Configuracion" to "CONFIGURACION",
        "Notificaciones" to "NOTIFICACIONES",
        "Mensajes automaticos" to "NOTIFICACIONES",
        "Auditoria" to "AUDITORIA",
    )

    /**
     * Secciones de «Más» por rol (aproximación por defecto). DUEÑO/ENCARGADO ven todo; los empleados solo
     * lo que su puesto usa, para no ofrecer lo que da 403. El filtro **preciso** —que respete lo que el
     * dueño concede a CADA empleado— necesita `GET /empleados/me/permisos` (lote de backend): esto es el
     * default por rol mientras tanto.
     */
    private fun seccionesDe(rol: Rol): Set<String> = when (rol) {
        Rol.DUENO, Rol.ENCARGADO -> secciones.toSet()
        Rol.MOSTRADOR -> setOf(
            "Rentas", "Devoluciones", "Pagos y cobros", "Caja / turnos", "Reembolsos", "Notificaciones",
        )
        Rol.ATENCION -> setOf(
            "Rentas", "Devoluciones", "Reembolsos", "Pagos y cobros", "Notificaciones",
        )
        Rol.BODEGA -> setOf("Devoluciones", "Notificaciones")
        else -> emptySet()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMasBinding.bind(view)

        // Hasta conocer el contexto se pinta todo; al llegar rol/permisos se filtra (evita un parpadeo vacio).
        pintarSecciones(secciones)
        observar(vm.rol) { rolActual = it; refiltrar() }
        // Filtro fino por las capacidades propias (paso 5); más preciso que el default por rol.
        observar(vm.misSecciones) { seccionesPermitidas = it; refiltrar() }

        // Selector de sucursal activa (A3): visible solo si hay más de una.
        binding.botonSucursal.setOnClickListener { elegirSucursalDialogo() }
        observar(vm.sucursales) { lista ->
            sucursalesActuales = lista
            val hayVarias = lista.size > 1
            binding.botonSucursal.isVisible = hayVarias
            binding.notaSucursal.isVisible = hayVarias
            actualizarTextoSucursal()
        }

        binding.botonComprar.setOnClickListener { vm.irAComprar() }
        observar(vm.irAComprar) { ir ->
            if (ir) requireActivity().findNavController(R.id.nav_host).irAHome(ModoApp.CLIENTE)
        }

        binding.botonLogout.setOnClickListener { vm.cerrarSesion() }
        observar(vm.cerrada) { cerrada ->
            if (cerrada) requireActivity().findNavController(R.id.nav_host).irALogin()
        }
    }

    /** Filtra las secciones por las capacidades propias (si ya cargaron) o, si no, por el default del rol. */
    private fun refiltrar() {
        val permitidas = seccionesPermitidas
        val rol = rolActual
        val visibles = secciones.filter { s ->
            when {
                permitidas != null -> seccionEnumDe[s] in permitidas
                rol != null -> s in seccionesDe(rol)
                else -> true
            }
        }
        pintarSecciones(visibles)
    }

    private fun actualizarTextoSucursal() {
        val activa = sucursalesActuales.firstOrNull { it.id?.toString() == vm.sucursalActivaId() }
        binding.botonSucursal.text = "Sucursal activa: ${activa?.nombre ?: "Todas"}"
    }

    private fun elegirSucursalDialogo() {
        val opciones = (listOf("Todas") + sucursalesActuales.map { it.nombre.orEmpty() }).toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sucursal activa")
            .setItems(opciones) { _, i ->
                vm.elegirSucursal(if (i == 0) null else sucursalesActuales[i - 1].id?.toString())
                actualizarTextoSucursal()
            }
            .show()
    }

    private fun pintarSecciones(visibles: List<String>) {
        binding.contenedorSecciones.removeAllViews()
        visibles.forEach { nombre ->
            val fila = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_seccion, binding.contenedorSecciones, false) as TextView
            fila.text = nombre
            fila.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_right, 0)
            fila.setOnClickListener { abrirSeccion(nombre) }
            binding.contenedorSecciones.addView(fila)
        }
    }

    private fun abrirSeccion(titulo: String) {
        // Secciones ya implementadas: se navega a su pantalla real; el resto, al placeholder.
        val destino = when (titulo) {
            "Rentas" -> R.id.rentasFragment
            "Devoluciones" -> R.id.devolucionesFragment
            "Pagos y cobros" -> R.id.pagosFragment
            "Caja / turnos" -> R.id.cajaFragment
            "Reembolsos" -> R.id.reembolsosFragment
            "Reportes" -> R.id.reportesFragment
            "Empleados" -> R.id.empleadosFragment
            "Sucursales" -> R.id.sucursalesFragment
            "Identidad de la tienda" -> R.id.identidadTiendaFragment
            "Configuracion" -> R.id.configuracionFragment
            "Notificaciones" -> R.id.notificacionesFragment
            "Mensajes automaticos" -> R.id.plantillasFragment
            "Auditoria" -> R.id.auditoriaFragment
            else -> null
        }
        if (destino != null) {
            findNavController().navigate(destino)
        } else {
            findNavController().navigate(
                R.id.proximamenteFragment,
                bundleOf(ProximamenteFragment.ARG_TITULO to titulo),
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
