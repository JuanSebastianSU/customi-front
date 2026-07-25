package com.costumi.app.ui.gestion.empleados

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentPermisosEmpleadoBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import dagger.hilt.android.AndroidEntryPoint

/** Matriz de permisos de un empleado: activar/desactivar Ver y Operar por sección. */
@AndroidEntryPoint
class PermisosEmpleadoFragment : Fragment(R.layout.fragment_permisos_empleado) {

    private val vm: PermisosEmpleadoViewModel by viewModels()
    private var _binding: FragmentPermisosEmpleadoBinding? = null
    private val binding get() = _binding!!
    private val adapter = PermisoAdapter { clave, concedido -> vm.establecer(clave, concedido) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPermisosEmpleadoBinding.bind(view)
        binding.toolbar.subtitle = vm.email
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.lista.adapter = adapter

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "No hay permisos configurables.") { adapter.submitList(it) }
        }
        observar(vm.error) { mostrarMensaje(it) }
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_ID = "empleadoId"
        const val ARG_EMAIL = "empleadoEmail"
    }
}
