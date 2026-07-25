package com.costumi.app.ui.gestion.clientes

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentClienteFichaBinding
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.ClienteResponse
import dagger.hilt.android.AndroidEntryPoint

/** Ficha de cliente: alta o edición de sus datos. */
@AndroidEntryPoint
class ClienteFichaFragment : Fragment(R.layout.fragment_cliente_ficha) {

    private val vm: ClienteFichaViewModel by viewModels()
    private var _binding: FragmentClienteFichaBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentClienteFichaBinding.bind(view)
        binding.toolbar.title = if (vm.esEdicion) "Editar cliente" else "Nuevo cliente"
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        prefill()
        // El correo identifica al cliente: se fija al crear y no se puede cambiar después.
        if (vm.esEdicion) {
            binding.editEmail.isEnabled = false
            binding.tilEmail.isEnabled = false
            binding.tilEmail.helperText = "El correo no se puede cambiar"
        }
        binding.botonGuardar.setOnClickListener { guardar() }
        configurarAccesosDeCuenta()

        observar(vm.guardando) { guardando ->
            binding.botonGuardar.isEnabled = !guardando
            binding.botonGuardar.text = if (guardando) "Guardando..." else "Guardar"
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                EventoFicha.Guardado -> {
                    setFragmentResult(RESULT_GUARDADO, Bundle.EMPTY)
                    findNavController().popBackStack()
                }
                is EventoFicha.Error -> mostrarMensaje(evento.mensaje)
                is EventoFicha.EstadoCuenta -> mostrarEstadoDeCuenta(
                    arguments?.getString(ARG_NOMBRE).orEmpty(), evento.estado,
                )
            }
        }
    }

    /**
     * Con el cliente delante, lo que se consulta es su cuenta y su historial. Estaban solo en el menu de
     * la lista: desde la ficha habia que volver atras y buscarlo de nuevo.
     */
    private fun configurarAccesosDeCuenta() {
        val id = arguments?.getString(ARG_ID)?.let { runCatching { java.util.UUID.fromString(it) }.getOrNull() }
        val hay = vm.esEdicion && id != null
        binding.tituloConsultar.isVisible = hay
        binding.botonEstadoCuenta.isVisible = hay
        binding.botonHistorial.isVisible = hay
        if (!hay) return
        binding.botonEstadoCuenta.setOnClickListener { vm.verEstadoCuenta(id!!) }
        binding.botonHistorial.setOnClickListener {
            findNavController().navigate(
                com.costumi.app.R.id.clienteHistorialFragment,
                ClienteHistorialFragment.args(id!!, arguments?.getString(ARG_NOMBRE)),
            )
        }
    }

    private fun prefill() {
        val a = arguments ?: return
        binding.editNombre.setText(a.getString(ARG_NOMBRE).orEmpty())
        binding.editDocumento.setText(a.getString(ARG_DOCUMENTO).orEmpty())
        binding.editTelefono.setText(a.getString(ARG_TELEFONO).orEmpty())
        binding.editEmail.setText(a.getString(ARG_EMAIL).orEmpty())
        binding.editDireccion.setText(a.getString(ARG_DIRECCION).orEmpty())
    }

    private fun guardar() {
        val nombre = binding.editNombre.text?.toString()?.trim().orEmpty()
        if (nombre.isBlank()) {
            binding.editNombre.error = "Ingresa un nombre"
            return
        }
        vm.guardar(
            nombre = nombre,
            telefono = binding.editTelefono.text?.toString()?.trim()?.ifBlank { null },
            email = binding.editEmail.text?.toString()?.trim()?.ifBlank { null },
            documento = binding.editDocumento.text?.toString()?.trim()?.ifBlank { null },
            direccion = binding.editDireccion.text?.toString()?.trim()?.ifBlank { null },
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_NOMBRE = "nombre"
        const val ARG_TELEFONO = "telefono"
        const val ARG_EMAIL = "email"
        const val ARG_DOCUMENTO = "documento"
        const val ARG_DIRECCION = "direccion"
        const val RESULT_GUARDADO = "cliente_guardado"

        fun argsEditar(c: ClienteResponse) = bundleOf(
            ARG_ID to c.id?.toString(),
            ARG_NOMBRE to c.nombre,
            ARG_TELEFONO to c.telefono,
            ARG_EMAIL to c.email,
            ARG_DOCUMENTO to c.documento,
            ARG_DIRECCION to c.direccion,
        )
    }
}
