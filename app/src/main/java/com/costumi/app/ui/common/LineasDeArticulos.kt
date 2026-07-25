package com.costumi.app.ui.common

import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.costumi.app.core.comoPrecio
import com.costumi.app.databinding.ItemLineaArticuloBinding
import com.costumi.app.ui.cargarFoto
import java.math.BigDecimal
import java.util.UUID

/**
 * Las lineas de articulos de un pedido de mostrador: agregar, quitar, cantidad, precio y subtotal.
 * La usan **vender** y **rentar**, que son la misma operacion salvo por el [multiplicador] (los dias de
 * renta) y el rotulo del precio. Antes cada pantalla tenia su copia y solo una estaba rediseniada.
 */
class LineasDeArticulos(
    private val fragment: Fragment,
    private val contenedor: ViewGroup,
    private val hintPrecio: String = "Precio unitario",
    private val alCambiar: () -> Unit = {},
) {
    /** Una linea viva: la prenda (por id) y su presentacion; cantidad y precio viven en la vista. */
    class Linea(
        val binding: ItemLineaArticuloBinding,
        val prendaId: UUID,
        val nombre: String,
        val fotoUrl: String?,
        val stock: Int?,
        internal val selector: SelectorDeCantidad,
    ) {
        val cantidad get() = selector.cantidad
        val precio: BigDecimal?
            get() = binding.editPrecio.text?.toString()?.trim()?.replace(",", ".")?.toBigDecimalOrNull()
    }

    /** Lo minimo para reconstruir las lineas al volver de otra pantalla (p. ej. configurar un disfraz). */
    data class Guardada(
        val prendaId: UUID,
        val nombre: String,
        val fotoUrl: String?,
        val stock: Int?,
        val cantidad: Int,
        val precio: String,
    )

    private val _lineas = mutableListOf<Linea>()
    val lineas: List<Linea> get() = _lineas

    /**
     * Dias de renta (1 en una venta): multiplica cada subtotal y el total. Repinta, pero NO vuelve a
     * avisar: lo fija quien ya esta recalculando, y avisar ahi seria una recursion.
     */
    var multiplicador: Int = 1
        set(valor) {
            val acotado = valor.coerceAtLeast(1)
            if (field == acotado) return
            field = acotado
            _lineas.forEach { pintarSubtotal(it) }
        }

    /** Agrega la prenda; si ya esta en el pedido, le suma uno en vez de duplicar la linea. */
    fun agregarOSumar(prendaId: UUID, nombre: String, fotoUrl: String?, precio: String?, stock: Int?) {
        _lineas.firstOrNull { it.prendaId == prendaId }?.let { existente ->
            existente.selector.cantidad += 1
            return
        }
        agregar(prendaId, nombre, fotoUrl, precio, stock, cantidad = 1)
    }

    fun restaurar(guardadas: List<Guardada>) {
        guardadas.forEach { agregar(it.prendaId, it.nombre, it.fotoUrl, it.precio, it.stock, it.cantidad) }
    }

    fun guardar(): List<Guardada> = _lineas.map {
        Guardada(it.prendaId, it.nombre, it.fotoUrl, it.stock, it.cantidad, it.binding.editPrecio.text?.toString().orEmpty())
    }

    /** Suma de precio x cantidad x [multiplicador] de todas las lineas con precio. */
    fun total(): BigDecimal = _lineas.fold(BigDecimal.ZERO) { acc, linea ->
        val precio = linea.precio ?: return@fold acc
        acc + precio * BigDecimal(linea.cantidad) * BigDecimal(multiplicador)
    }

    fun estaVacio() = _lineas.isEmpty()

    fun limpiar() {
        _lineas.clear()
        contenedor.removeAllViews()
    }

    private fun agregar(
        prendaId: UUID,
        nombre: String,
        fotoUrl: String?,
        precio: String?,
        stock: Int?,
        cantidad: Int,
    ) {
        val binding = ItemLineaArticuloBinding.inflate(fragment.layoutInflater, contenedor, false)
        binding.foto.cargarFoto(fotoUrl)
        binding.nombre.text = nombre
        binding.tilPrecio.hint = hintPrecio
        if (!precio.isNullOrBlank()) binding.editPrecio.setText(precio)

        lateinit var linea: Linea
        val selector = SelectorDeCantidad(
            binding = binding.cantidad,
            maximo = stock?.takeIf { it > 0 },
            inicial = cantidad,
        ) {
            pintarSubtotal(linea)
            alCambiar()
        }
        linea = Linea(binding, prendaId, nombre, fotoUrl, stock, selector)

        binding.editPrecio.doAfterTextChanged { pintarSubtotal(linea); alCambiar() }
        binding.botonEliminar.setOnClickListener {
            contenedor.removeView(binding.root)
            _lineas.remove(linea)
            alCambiar()
        }
        _lineas.add(linea)
        contenedor.addView(binding.root)
        pintarSubtotal(linea)
        alCambiar()
    }

    private fun pintarSubtotal(linea: Linea) {
        val precio = linea.precio
        linea.binding.subtotal.text = precio
            ?.let { it * BigDecimal(linea.cantidad) * BigDecimal(multiplicador) }
            .comoPrecio()
            .orEmpty()
    }
}
