package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.EmpresaVitrinaResponse
import com.costumi.apiclient.models.PrendaVitrinaResponse
import com.costumi.apiclient.models.ProblemDetail
import com.costumi.apiclient.models.SucursalVitrinaResponse

interface MarketplaceControllerApi {
    /**
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
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param buscar  (optional)
     * @return [kotlin.collections.List<EmpresaVitrinaResponse>]
     */
    @GET("api/v1/marketplace/empresas")
    suspend fun empresas(@Query("buscar") buscar: kotlin.String? = null): Response<kotlin.collections.List<EmpresaVitrinaResponse>>

    /**
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
