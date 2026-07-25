package com.costumi.app.data.remote

import retrofit2.Response
import retrofit2.http.GET
import java.util.UUID

/**
 * Los carritos que el cliente dejo abiertos, en cualquier tienda (`GET /carritos/mios`, RF-16.2/16.3).
 * Se define a mano porque el endpoint es nuevo y el `:api-client` se genera del contrato ya desplegado;
 * cuando entre y se regenere, se puede reemplazar por el metodo generado.
 *
 * Se resuelve por el usuario del token, asi que no recibe ningun id.
 */
interface MisCarritosApi {

    @GET("api/v1/carritos/mios")
    suspend fun mios(): Response<List<CarritoAbiertoDto>>
}

/**
 * Un carrito abierto. Trae los tres datos que `GET /carritos` exige para abrirlo (empresa, sucursal y
 * tipo) y los nombres para poder pintarlo.
 *
 * Lleva las **unidades**, no el importe: el precio del carrito no esta guardado, lo calcula el backend al
 * valorizarlo. El importe aparece al abrir el carrito.
 */
data class CarritoAbiertoDto(
    val empresaId: UUID?,
    val empresaNombre: String?,
    val sucursalId: UUID?,
    val sucursalNombre: String?,
    val tipo: String?,
    val articulos: Int?,
)
