package com.costumi.app.ui.gestion.config

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.costumi.app.R
import com.costumi.app.databinding.FragmentConfiguracionBinding
import com.costumi.app.ui.mostrar
import com.costumi.app.ui.mostrarMensaje
import com.costumi.app.ui.observar
import com.costumi.apiclient.models.ConfiguracionRequest
import com.costumi.apiclient.models.ConfiguracionResponse
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/** Configuración de la empresa: switches, impuesto, moneda, recargo por retraso y reembolsos. */
@AndroidEntryPoint
class ConfiguracionFragment : Fragment(R.layout.fragment_configuracion) {

    private val vm: ConfiguracionViewModel by viewModels()
    private var _binding: FragmentConfiguracionBinding? = null
    private val binding get() = _binding!!
    private var preparado = false
    private var jsonParaExportar: String? = null

    // Guardar el respaldo en un archivo que elige el usuario (SAF).
    private val crearRespaldo = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = jsonParaExportar
        if (uri != null && json != null) escribirRespaldo(uri, json)
    }

    // Elegir un archivo de respaldo para restaurar.
    private val elegirRespaldo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { leerRespaldo(it) }
    }

    private val modos = listOf(
        "Acumulativa" to ConfiguracionRequest.ModoRecargoRetraso.ACUMULATIVA,
        "Fija" to ConfiguracionRequest.ModoRecargoRetraso.FIJA,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentConfiguracionBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.toolbar.inflateMenu(R.menu.menu_configuracion)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.accionExportar -> { vm.exportar(); true }
                R.id.accionImportar -> { elegirRespaldo.launch("application/json"); true }
                else -> false
            }
        }
        binding.dropModo.setSimpleItems(modos.map { it.first }.toTypedArray())
        binding.botonGuardar.setOnClickListener { guardar() }

        observar(vm.estado) { estado ->
            binding.stateView.mostrar(estado, vacio = "Sin configuracion.") { poblar(it) }
        }
        observar(vm.guardando) { g ->
            binding.botonGuardar.isEnabled = !g
            binding.botonGuardar.text = if (g) "Guardando..." else "Guardar"
        }
        observar(vm.eventos) { evento ->
            when (evento) {
                is EventoConfig.Guardada -> mostrarMensaje("Configuracion guardada.")
                is EventoConfig.Exportada -> {
                    jsonParaExportar = evento.json
                    crearRespaldo.launch("costumi-config.json")
                }
                is EventoConfig.Importada -> mostrarMensaje("Configuracion restaurada del respaldo.")
                is EventoConfig.Error -> mostrarMensaje(evento.mensaje)
            }
        }
    }

    private fun escribirRespaldo(uri: Uri, json: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    requireContext().contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }.isSuccess
            }
            mostrarMensaje(if (ok) "Respaldo guardado." else "No se pudo guardar el respaldo.")
        }
    }

    private fun leerRespaldo(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val json = withContext(Dispatchers.IO) {
                runCatching {
                    requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                }.getOrNull()
            }
            if (json.isNullOrBlank()) mostrarMensaje("No se pudo leer el archivo.")
            else vm.importar(json)
        }
    }

    private fun poblar(c: ConfiguracionResponse) {
        binding.switchConteoStock.isChecked = c.conteoStock == true
        binding.switchMultas.isChecked = c.multasActivo == true
        binding.switchMultiSucursal.isChecked = c.multiSucursal == true
        binding.switchPagoLinea.isChecked = c.pagoEnLinea == true
        binding.switchReembolsos.isChecked = c.reembolsosActivos == true
        binding.editImpuesto.setText(c.tasaImpuesto?.toPlainString() ?: "0")
        binding.editMoneda.setText(c.moneda ?: "COP")
        binding.editRecargo.setText(c.recargoPorRetrasoPorDia?.toPlainString() ?: "0")
        binding.editVentana.setText((c.ventanaReembolsoDias ?: 0).toString())
        val modo = modos.firstOrNull { it.second.value == c.modoRecargoRetraso?.value }?.first ?: modos.first().first
        binding.dropModo.setText(modo, false)
        preparado = true
    }

    private fun guardar() {
        if (!preparado) return
        val impuesto = binding.editImpuesto.text?.toString()?.trim()?.replace(",", ".")?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val recargo = binding.editRecargo.text?.toString()?.trim()?.replace(",", ".")?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val ventana = binding.editVentana.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val moneda = binding.editMoneda.text?.toString()?.trim()?.ifBlank { "COP" } ?: "COP"
        val modo = modos.firstOrNull { it.first == binding.dropModo.text?.toString() }?.second ?: modos.first().second
        // Reemplazo total: todos los campos explícitos (los 4 switches no aceptan null).
        vm.guardar(
            ConfiguracionRequest(
                conteoStock = binding.switchConteoStock.isChecked,
                multasActivo = binding.switchMultas.isChecked,
                multiSucursal = binding.switchMultiSucursal.isChecked,
                pagoEnLinea = binding.switchPagoLinea.isChecked,
                tasaImpuesto = impuesto,
                moneda = moneda,
                recargoPorRetrasoPorDia = recargo,
                modoRecargoRetraso = modo,
                reembolsosActivos = binding.switchReembolsos.isChecked,
                ventanaReembolsoDias = ventana,
            ),
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
