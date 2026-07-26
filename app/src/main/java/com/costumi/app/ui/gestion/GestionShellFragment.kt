package com.costumi.app.ui.gestion

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.costumi.app.R
import com.costumi.app.core.Rol
import com.costumi.app.databinding.FragmentGestionShellBinding
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint

/**
 * Contenedor del modo GESTION: barra inferior (Panel / Inventario / Ventas / Clientes / Mas)
 * sobre un NavHost anidado (nav_gestion). La barra se filtra segun el [Rol] del usuario (§8 del
 * diseno) y se oculta en pantallas abiertas por encima (secciones de "Mas").
 */
@AndroidEntryPoint
class GestionShellFragment : Fragment(R.layout.fragment_gestion_shell) {

    private val vm: GestionShellViewModel by viewModels()
    private var _binding: FragmentGestionShellBinding? = null
    private val binding get() = _binding!!
    private lateinit var navController: NavController

    /** Pestañas del rol actual (destinos que muestran la barra inferior). Se arma al conocer el rol. */
    private var pestanas: List<Int> = emptyList()

    /**
     * Permiso de notificaciones (Android 13+). Antes solo se pedía en el shell de CLIENTE, así que un
     * dueño/empleado en modo gestión nunca lo concedía y las push no se mostraban en un teléfono real
     * (el sistema deja la app en importancia NONE). Se pide también aquí.
     */
    private val pedirPermisoDeNotificaciones =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    private fun asegurarPermisoDeNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
        val concedido = androidx.core.content.ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!concedido) pedirPermisoDeNotificaciones.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentGestionShellBinding.bind(view)
        asegurarPermisoDeNotificaciones()

        val navHost = childFragmentManager.findFragmentById(R.id.gestion_nav_host) as NavHostFragment
        navController = navHost.navController
        // La barra se arma según el rol; hasta entonces se oculta para no mostrar pestañas ajenas.
        binding.bottomNav.isVisible = false

        navController.addOnDestinationChangedListener { _, destino, _ ->
            binding.bottomNav.isVisible = destino.id in pestanas
        }

        observar(vm.rol) { rol -> rol?.let(::configurarParaRol) }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navController.popBackStack()) {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    /**
     * Arma la barra inferior con el menú **propio del rol** (H3): cada rol tiene a un toque sus pantallas
     * de a diario, en vez de una barra casi vacía con todo hundido en "Mas". Si la pantalla actual no es
     * una pestaña del rol (p. ej. arrancó en el Panel y es BODEGA), salta a su primera pestaña.
     *
     * El filtro **fino por permisos por-empleado** necesita `GET /empleados/me/permisos` (lote de backend);
     * hoy se aproxima por rol.
     */
    private fun configurarParaRol(rol: Rol) {
        binding.bottomNav.menu.clear()
        binding.bottomNav.inflateMenu(menuDe(rol))
        binding.bottomNav.setupWithNavController(navController)
        pestanas = pestanasDe(rol)

        val actual = navController.currentDestination?.id
        binding.bottomNav.isVisible = actual in pestanas
        if (actual == null || actual !in pestanas) {
            pestanas.firstOrNull()?.let { destino ->
                navController.navigate(
                    destino,
                    null,
                    navOptions {
                        popUpTo(R.id.nav_gestion) { inclusive = true }
                        launchSingleTop = true
                    },
                )
            }
        }
    }

    /** Menú inferior propio de cada rol. */
    private fun menuDe(rol: Rol): Int = when (rol) {
        Rol.DUENO, Rol.ENCARGADO -> R.menu.menu_gestion_bottom
        Rol.MOSTRADOR, Rol.ATENCION -> R.menu.menu_gestion_mostrador
        else -> R.menu.menu_gestion_bodega
    }

    /** Destinos-pestaña de cada rol, EN ORDEN (el primero es el aterrizaje). Debe coincidir con [menuDe]. */
    private fun pestanasDe(rol: Rol): List<Int> = when (rol) {
        Rol.DUENO, Rol.ENCARGADO -> listOf(
            R.id.dashboardFragment,
            R.id.inventarioFragment,
            R.id.ventasFragment,
            R.id.clientesFragment,
            R.id.masFragment,
        )
        Rol.MOSTRADOR, Rol.ATENCION -> listOf(
            R.id.ventasFragment,
            R.id.rentasFragment,
            R.id.clientesFragment,
            R.id.masFragment,
        )
        else -> listOf(
            R.id.inventarioFragment,
            R.id.masFragment,
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
