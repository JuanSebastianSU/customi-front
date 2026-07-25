package com.costumi.app.data.remote

import com.costumi.apiclient.models.EmpresaResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * La propia tienda del usuario (`GET /empresas/mia`, RF-15.1). Se define a mano porque el endpoint es
 * nuevo y el `:api-client` se genera del contrato del backend ya desplegado; en cuanto entre y se
 * regenere, esto se puede reemplazar por el metodo generado (mismo caso que [ReporteExportApi]).
 *
 * La empresa sale del token, asi que no recibe ningun id.
 */
interface MiEmpresaApi {

    @GET("api/v1/empresas/mia")
    suspend fun mia(): Response<EmpresaResponse>
}
