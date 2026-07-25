package com.costumi.apiclient.apis

import com.costumi.apiclient.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.costumi.apiclient.models.ConteoDeDependenciasResponse
import com.costumi.apiclient.models.ProblemDetail

interface ConteoDeDependenciasControllerApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param categoriaId 
     * @return [ConteoDeDependenciasResponse]
     */
    @GET("api/v1/categorias/{categoriaId}/prendas/conteo")
    suspend fun deCategoria(@Path("categoriaId") categoriaId: java.util.UUID): Response<ConteoDeDependenciasResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param tipoId 
     * @return [ConteoDeDependenciasResponse]
     */
    @GET("api/v1/tipos-etiqueta/{tipoId}/prendas/conteo")
    suspend fun deTipoEtiqueta(@Path("tipoId") tipoId: java.util.UUID): Response<ConteoDeDependenciasResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *  - 0: Error de la API en formato RFC 7807 (application/problem+json).
     *
     * @param tipoId 
     * @param valorId 
     * @return [ConteoDeDependenciasResponse]
     */
    @GET("api/v1/tipos-etiqueta/{tipoId}/valores/{valorId}/prendas/conteo")
    suspend fun deValorEtiqueta(@Path("tipoId") tipoId: java.util.UUID, @Path("valorId") valorId: java.util.UUID): Response<ConteoDeDependenciasResponse>

}
