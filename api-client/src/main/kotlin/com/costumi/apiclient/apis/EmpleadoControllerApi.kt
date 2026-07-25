package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.AsignarSucursalesRequest
import com.costumi.apiclient.models.CambiarRolRequest
import com.costumi.apiclient.models.EmpleadoResponse
import com.costumi.apiclient.models.InvitacionPendienteResponse
import com.costumi.apiclient.models.InvitacionResponse
import com.costumi.apiclient.models.InvitarEmpleadoRequest
import com.costumi.apiclient.models.MembresiaEstadoResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RespuestaPaginadaEmpleadoDetalleResponse

interface EmpleadoControllerApi {
    /**
     * POST api/v1/empleados/{usuarioId}/activar
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
     * PUT api/v1/empleados/{usuarioId}/sucursales
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
     * PUT api/v1/empleados/{usuarioId}/rol
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
     * DELETE api/v1/empleados/invitaciones/{invitacionId}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param invitacionId 
     * @return [Unit]
     */
    @DELETE("api/v1/empleados/invitaciones/{invitacionId}")
    suspend fun cancelarInvitacion(@Path("invitacionId") invitacionId: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/empleados/{usuarioId}/desactivar
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
     * GET api/v1/empleados/invitaciones
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @return [kotlin.collections.List<InvitacionPendienteResponse>]
     */
    @GET("api/v1/empleados/invitaciones")
    suspend fun invitacionesPendientes(): Response<kotlin.collections.List<InvitacionPendienteResponse>>

    /**
     * POST api/v1/empleados
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param invitarEmpleadoRequest 
     * @return [InvitacionResponse]
     */
    @POST("api/v1/empleados")
    suspend fun invitar(@Body invitarEmpleadoRequest: InvitarEmpleadoRequest): Response<InvitacionResponse>

    /**
     * GET api/v1/empleados
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
     * POST api/v1/empleados/{usuarioId}/quitar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param usuarioId 
     * @return [MembresiaEstadoResponse]
     */
    @POST("api/v1/empleados/{usuarioId}/quitar")
    suspend fun quitar(@Path("usuarioId") usuarioId: java.util.UUID): Response<MembresiaEstadoResponse>

    /**
     * POST api/v1/empleados/{usuarioId}/reactivar
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param usuarioId 
     * @return [MembresiaEstadoResponse]
     */
    @POST("api/v1/empleados/{usuarioId}/reactivar")
    suspend fun reactivar1(@Path("usuarioId") usuarioId: java.util.UUID): Response<MembresiaEstadoResponse>

    /**
     * GET api/v1/empleados/{usuarioId}/sucursales
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

    /**
     * POST api/v1/empleados/{usuarioId}/suspender
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param usuarioId 
     * @return [MembresiaEstadoResponse]
     */
    @POST("api/v1/empleados/{usuarioId}/suspender")
    suspend fun suspender1(@Path("usuarioId") usuarioId: java.util.UUID): Response<MembresiaEstadoResponse>

}
