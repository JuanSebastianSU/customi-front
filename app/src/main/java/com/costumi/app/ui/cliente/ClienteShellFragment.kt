package com.costumi.app.ui.cliente

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentClienteShellBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Contenedor del modo CLIENTE: barra inferior (Explorar / Mis pedidos / Perfil) sobre un NavHost
 * anidado (nav_cliente). El flujo de compra (tienda/detalle/carrito) se abre encima y oculta la barra.
 */
@AndroidEntryPoint
class ClienteShellFragment : Fragment(R.layout.fragment_cliente_shell) {

    private var _binding: FragmentClienteShellBinding? = null
    private val binding get() = _binding!!
    private lateinit var navController: NavController

    private val pestanas = setOf(
        R.id.explorarFragment,
        R.id.misCarritosFragment,
        R.id.misPedidosFragment,
        R.id.perfilFragment,
    )

    /**
     * Desde Android 13 las notificaciones necesitan permiso en tiempo de ejecucion: sin pedirlo, el
     * sistema deja la app en importancia NONE y las push **no se muestran**, aunque lleguen. Se pide una
     * sola vez al entrar; si el usuario dice que no, la app sigue funcionando igual.
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
        asegurarPermisoDeNotificaciones()
        _binding = FragmentClienteShellBinding.bind(view)

        val navHost = childFragmentManager.findFragmentById(R.id.cliente_nav_host) as NavHostFragment
        navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)

        // La barra solo se muestra en las pestañas; en el flujo de compra se oculta.
        navController.addOnDestinationChangedListener { _, destino, _ ->
            binding.bottomNav.isVisible = destino.id in pestanas
        }

        // Atrás: primero navega dentro del grafo del cliente; si no hay a dónde, sale del shell.
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
