package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.AgregarItemRequest
import com.costumi.apiclient.models.CarritoAbiertoResponse
import com.costumi.apiclient.models.CarritoResponse
import com.costumi.apiclient.models.CheckoutRentaResponse
import com.costumi.apiclient.models.CheckoutRequest
import com.costumi.apiclient.models.CheckoutResponse
import com.costumi.apiclient.models.EditarCantidadRequest
import com.costumi.apiclient.models.ProblemDetail

interface CarritoControllerApi {
    /**
     * POST api/v1/carritos/items
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param agregarItemRequest 
     * @return [CarritoResponse]
     */
    @POST("api/v1/carritos/items")
    suspend fun agregarItem(@Body agregarItemRequest: AgregarItemRequest): Response<CarritoResponse>

    /**
     * POST api/v1/carritos/checkout
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param checkoutRequest 
     * @return [CheckoutResponse]
     */
    @POST("api/v1/carritos/checkout")
    suspend fun checkout(@Body checkoutRequest: CheckoutRequest): Response<CheckoutResponse>

    /**
     * POST api/v1/carritos/checkout-renta
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param checkoutRequest 
     * @return [CheckoutRentaResponse]
     */
    @POST("api/v1/carritos/checkout-renta")
    suspend fun checkoutRenta(@Body checkoutRequest: CheckoutRequest): Response<CheckoutRentaResponse>

    /**
     * PUT api/v1/carritos/items/{lineaId}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param lineaId 
     * @param editarCantidadRequest 
     * @return [CarritoResponse]
     */
    @PUT("api/v1/carritos/items/{lineaId}")
    suspend fun editarCantidad(@Path("lineaId") lineaId: java.util.UUID, @Body editarCantidadRequest: EditarCantidadRequest): Response<CarritoResponse>

    /**
     * GET api/v1/carritos/mios
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<CarritoAbiertoResponse>]
     */
    @GET("api/v1/carritos/mios")
    suspend fun mios(): Response<kotlin.collections.List<CarritoAbiertoResponse>>


    /**
    * enum for parameter tipo
    */
    enum class TipoPendiente(val value: kotlin.String) {
        @SerializedName(value = "RENTA") RENTA("RENTA"),
        @SerializedName(value = "VENTA") VENTA("VENTA")
    }

    /**
     * GET api/v1/carritos
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param sucursalId 
     * @param tipo 
     * @param empresaId  (optional)
     * @param clienteId  (optional)
     * @return [CarritoResponse]
     */
    @GET("api/v1/carritos")
    suspend fun pendiente(@Query("sucursalId") sucursalId: java.util.UUID, @Query("tipo") tipo: TipoPendiente, @Query("empresaId") empresaId: java.util.UUID? = null, @Query("clienteId") clienteId: java.util.UUID? = null): Response<CarritoResponse>


    /**
    * enum for parameter tipo
    */
    enum class TipoQuitarItem(val value: kotlin.String) {
        @SerializedName(value = "RENTA") RENTA("RENTA"),
        @SerializedName(value = "VENTA") VENTA("VENTA")
    }

    /**
     * DELETE api/v1/carritos/items/{lineaId}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param lineaId 
     * @param sucursalId 
     * @param tipo 
     * @param empresaId  (optional)
     * @param clienteId  (optional)
     * @return [CarritoResponse]
     */
    @DELETE("api/v1/carritos/items/{lineaId}")
    suspend fun quitarItem(@Path("lineaId") lineaId: java.util.UUID, @Query("sucursalId") sucursalId: java.util.UUID, @Query("tipo") tipo: TipoQuitarItem, @Query("empresaId") empresaId: java.util.UUID? = null, @Query("clienteId") clienteId: java.util.UUID? = null): Response<CarritoResponse>

}
