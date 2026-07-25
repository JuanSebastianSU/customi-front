package com.costumi.app.ui.gestion.disfraces

import com.costumi.apiclient.models.SeleccionSlotDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Carrito en memoria para armar un pedido de VARIOS disfraces distintos antes de confirmarlo (RF-3.1). */
@Singleton
class PedidoDisfracesStore @Inject constructor() {

    /** Un disfraz ya configurado (piezas elegidas + cantidad) listo para sumarse al pedido. */
    data class Item(
        val disfrazId: UUID,
        val nombre: String,
        val cantidad: Int,
        val selecciones: List<SeleccionSlotDto>,
    )

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items = _items.asStateFlow()

    fun agregar(item: Item) {
        _items.value = _items.value + item
    }

    fun quitar(indice: Int) {
        _items.value = _items.value.filterIndexed { i, _ -> i != indice }
    }

    fun limpiar() {
        _items.value = emptyList()
    }
}
