package dev.raflos.cocina.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Respuesta de `GET /repos/{owner}/{repo}/contents/{path}`: el archivo en base64 + el `sha`
 * del blob, que hace de control de concurrencia optimista para el PUT posterior. */
@Serializable
data class GithubContent(
    val content: String? = null,
    val encoding: String? = null,
    val sha: String,
    val path: String? = null,
)

/** Cuerpo de `PUT /repos/{owner}/{repo}/contents/{path}`. `sha` va en null si el archivo
 * todavía no existe en el repo (GitHub lo crea). */
@Serializable
data class GithubPutRequest(
    val message: String,
    val content: String,
    val sha: String? = null,
    val branch: String = "main",
)

/** El PUT devuelve el commit nuevo y el blob resultante; solo nos interesa su `sha`. */
@Serializable
data class GithubPutResponse(
    val content: GithubContent? = null,
)

interface GithubContentsApi {
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Query("ref") ref: String = "main",
    ): GithubContent

    /** `Response<...>` (y no el body pelado) para poder mirar el código: 409 = `sha` viejo. */
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body body: GithubPutRequest,
    ): Response<GithubPutResponse>
}
