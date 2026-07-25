package com.costumi.app.ui.gestion.clientes

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.costumi.app.R
import com.costumi.app.databinding.FragmentClientesBinding
import com.costumi.app.ui.gestion.inventario.PrendasLoadStateAdapter
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.ClienteResponse
import dagger.hilt.android.AndroidEntryPoint

/** Clientes — lista paginada con búsqueda, filtro de archivados, lista negra, archivar e historial. */
@AndroidEntryPoint
class ClientesFragment : Fragment(R.layout.fragment_clientes) {

    private val vm: ClientesViewModel by viewModels()
    private var _binding: FragmentClientesBinding? = null
    private val binding get() = _binding!!

    private val adapter = ClienteAdapter(
        alEditar = { abrirFicha(it) },
        alVerHistorial = { abrirHistorial(it) },
        alVerEstadoCuenta = { vm.verEstadoCuenta(it) },
        alAlternarListaNegra = { vm.alternarListaNegra(it) },
        alAlternarArchivado = { vm.alternarArchivado(it) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentClientesBinding.bind(view)

        binding.lista.adapter = adapter.withLoadStateFooter(
            PrendasLoadStateAdapter { adapter.retry() },
        )

        setFragmentResultListener(ClienteFichaFragment.RESULT_GUARDADO) { _, _ -> adapter.refresh() }

        binding.fabNueva.setOnClickListener {
            findNavController().navigate(R.id.clienteFichaFragment)
        }

        var pendiente: Runnable? = null
        binding.editBuscar.doAfterTextChanged { texto ->
            pendiente?.let { binding.editBuscar.removeCallbacks(it) }
            val r = Runnable { vm.buscar(texto?.toString().orEmpty()) }
            pendiente = r
            binding.editBuscar.postDelayed(r, 350)
        }

        binding.chipArchivados.setOnCheckedChangeListener { _, checked -> vm.verArchivados(checked) }

        binding.chipsCartera.setOnCheckedStateChangeListener { _, ids ->
            val filtro = when (ids.firstOrNull()) {
                R.id.carteraPendientes -> "PENDIENTES"
                R.id.carteraVencidas -> "VENCIDAS"
                R.id.carteraMultas -> "MULTAS"
                R.id.carteraSaldos -> "SALDOS"
                else -> null
            }
            vm.filtrar(filtro)
        }

        observar(vm.clientes) { adapter.submitData(viewLifecycleOwner.lifecycle, it) }
        observar(adapter.loadStateFlow) { estados -> pintarEstado(estados.refresh) }

        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoCliente.Info -> {
                    mostrarMensaje(evento.mensaje)
                    adapter.refresh()
                }
                is EventoCliente.Error -> mostrarMensaje(evento.mensaje)
                is EventoCliente.EstadoCuenta ->
                    mostrarEstadoDeCuenta(evento.cliente.nombre.orEmpty(), evento.estado)
            }
        }
    }

    private fun abrirFicha(cliente: ClienteResponse) {
        findNavController().navigate(R.id.clienteFichaFragment, ClienteFichaFragment.argsEditar(cliente))
    }

    private fun abrirHistorial(cliente: ClienteResponse) {
        val id = cliente.id ?: return
        findNavController().navigate(
            R.id.clienteHistorialFragment,
            ClienteHistorialFragment.args(id, cliente.nombre),
        )
    }

    private fun pintarEstado(refresh: LoadState) {
        when (refresh) {
            is LoadState.Loading -> if (adapter.itemCount == 0) binding.stateView.cargando()
            is LoadState.Error ->
                binding.stateView.error(refresh.error.localizedMessage ?: "No se pudieron cargar los clientes.") {
                    adapter.refresh()
                }
            is LoadState.NotLoading ->
                if (adapter.itemCount == 0) {
                    binding.stateView.vacio("No hay clientes. Crea el primero con el boton +.")
                } else {
                    binding.stateView.ocultar()
                }
        }
    }

    override fun onDestroyView() {
        binding.lista.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
