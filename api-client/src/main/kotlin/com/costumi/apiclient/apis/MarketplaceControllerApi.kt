package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.CategoriaVitrinaResponse
import com.costumi.apiclient.models.DisfrazDestacadoResponse
import com.costumi.apiclient.models.EmpresaVitrinaResponse
import com.costumi.apiclient.models.HorarioVitrinaResponse
import com.costumi.apiclient.models.PrendaVitrinaResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.RespuestaPaginadaEmpresaVitrinaResponse
import com.costumi.apiclient.models.SucursalVitrinaResponse

interface MarketplaceControllerApi {
    /**
     * GET api/v1/marketplace/empresas/{empresaId}/catalogo
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @param categoria  (optional)
     * @return [kotlin.collections.List<PrendaVitrinaResponse>]
     */
    @GET("api/v1/marketplace/empresas/{empresaId}/catalogo")
    suspend fun catalogo1(@Path("empresaId") empresaId: java.util.UUID, @Query("categoria") categoria: java.util.UUID? = null): Response<kotlin.collections.List<PrendaVitrinaResponse>>

    /**
     * GET api/v1/marketplace/empresas/{empresaId}/categorias
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @return [kotlin.collections.List<CategoriaVitrinaResponse>]
     */
    @GET("api/v1/marketplace/empresas/{empresaId}/categorias")
    suspend fun categorias(@Path("empresaId") empresaId: java.util.UUID): Response<kotlin.collections.List<CategoriaVitrinaResponse>>

    /**
     * GET api/v1/marketplace/destacados
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param limite  (optional, default to 10)
     * @return [kotlin.collections.List<DisfrazDestacadoResponse>]
     */
    @GET("api/v1/marketplace/destacados")
    suspend fun destacados(@Query("limite") limite: kotlin.Int? = 10): Response<kotlin.collections.List<DisfrazDestacadoResponse>>

    /**
     * GET api/v1/marketplace/empresas/{empresaId}
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @return [EmpresaVitrinaResponse]
     */
    @GET("api/v1/marketplace/empresas/{empresaId}")
    suspend fun empresa(@Path("empresaId") empresaId: java.util.UUID): Response<EmpresaVitrinaResponse>

    /**
     * GET api/v1/marketplace/empresas
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param buscar  (optional)
     * @param pagina  (optional)
     * @param tamano  (optional)
     * @return [RespuestaPaginadaEmpresaVitrinaResponse]
     */
    @GET("api/v1/marketplace/empresas")
    suspend fun empresas(@Query("buscar") buscar: kotlin.String? = null, @Query("pagina") pagina: kotlin.Int? = null, @Query("tamano") tamano: kotlin.Int? = null): Response<RespuestaPaginadaEmpresaVitrinaResponse>

    /**
     * GET api/v1/marketplace/empresas/{empresaId}/horario
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @return [kotlin.collections.List<HorarioVitrinaResponse>]
     */
    @GET("api/v1/marketplace/empresas/{empresaId}/horario")
    suspend fun horario(@Path("empresaId") empresaId: java.util.UUID): Response<kotlin.collections.List<HorarioVitrinaResponse>>

    /**
     * GET api/v1/marketplace/empresas/{empresaId}/sucursales
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param empresaId 
     * @return [kotlin.collections.List<SucursalVitrinaResponse>]
     */
    @GET("api/v1/marketplace/empresas/{empresaId}/sucursales")
    suspend fun sucursales(@Path("empresaId") empresaId: java.util.UUID): Response<kotlin.collections.List<SucursalVitrinaResponse>>

}
