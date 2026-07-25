package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.AltaDeEmpleadoRequest
import com.costumi.apiclient.models.AsignarSucursalesRequest
import com.costumi.apiclient.models.CambiarRolRequest
import com.costumi.apiclient.models.EmpleadoResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RespuestaPaginadaEmpleadoDetalleResponse

interface EmpleadoControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param usuarioId 
     * @return [EmpleadoResponse]
     */
    @POST("api/v1/empleados/{usuarioId}/activar")
    suspend fun activar2(@Path("usuarioId") usuarioId: java.util.UUID): Response<EmpleadoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param usuarioId 
     * @param asignarSucursalesRequest 
     * @return [kotlin.collections.List<java.util.UUID>]
     */
    @PUT("api/v1/empleados/{usuarioId}/sucursales")
    suspend fun asignarSucursales(@Path("usuarioId") usuarioId: java.util.UUID, @Body asignarSucursalesRequest: AsignarSucursalesRequest): Response<kotlin.collections.List<java.util.UUID>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param usuarioId 
     * @param cambiarRolRequest 
     * @return [EmpleadoResponse]
     */
    @PUT("api/v1/empleados/{usuarioId}/rol")
    suspend fun cambiarRol(@Path("usuarioId") usuarioId: java.util.UUID, @Body cambiarRolRequest: CambiarRolRequest): Response<EmpleadoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param altaDeEmpleadoRequest 
     * @return [EmpleadoResponse]
     */
    @POST("api/v1/empleados")
    suspend fun crear4(@Body altaDeEmpleadoRequest: AltaDeEmpleadoRequest): Response<EmpleadoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param usuarioId 
     * @return [EmpleadoResponse]
     */
    @POST("api/v1/empleados/{usuarioId}/desactivar")
    suspend fun desactivar(@Path("usuarioId") usuarioId: java.util.UUID): Response<EmpleadoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param buscar  (optional)
     * @param pagina  (optional)
     * @param tamano  (optional)
     * @return [RespuestaPaginadaEmpleadoDetalleResponse]
     */
    @GET("api/v1/empleados")
    suspend fun listar10(@Query("buscar") buscar: kotlin.String? = null, @Query("pagina") pagina: kotlin.Int? = null, @Query("tamano") tamano: kotlin.Int? = null): Response<RespuestaPaginadaEmpleadoDetalleResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param usuarioId 
     * @return [kotlin.collections.List<java.util.UUID>]
     */
    @GET("api/v1/empleados/{usuarioId}/sucursales")
    suspend fun sucursalesDe(@Path("usuarioId") usuarioId: java.util.UUID): Response<kotlin.collections.List<java.util.UUID>>

}
