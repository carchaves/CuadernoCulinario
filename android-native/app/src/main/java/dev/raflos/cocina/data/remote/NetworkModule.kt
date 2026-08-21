package dev.raflos.cocina.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private const val GITHUB_BASE_URL = "https://api.github.com/"

/** Cliente de la API de GitHub. Un PAT no se refresca (si deja de servir se pega uno nuevo en
 * Ajustes), así que acá no hay `Authenticator` ni lógica de renovación: solo los headers. */
class NetworkModule(private val tokenStore: GithubTokenStore) {

    private val jsonMediaType = "application/json".toMediaType()

    private val githubHeadersInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
        tokenStore.getToken()?.let { builder.header("Authorization", "Bearer $it") }
        chain.proceed(builder.build())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(githubHeadersInterceptor)
        .build()

    val contentsApi: GithubContentsApi = Retrofit.Builder()
        .baseUrl(GITHUB_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory(jsonMediaType))
        .build()
        .create(GithubContentsApi::class.java)

    companion object {
        /** Chequeo liviano del PAT antes de guardarlo (pantalla de Ajustes): 200 significa que
         * el token existe y alcanza a ver el repo. Va por fuera del cliente de arriba porque
         * todavía no hay nada guardado en [GithubTokenStore]. */
        suspend fun validateToken(owner: String, repo: String, pat: String): Boolean =
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url("${GITHUB_BASE_URL}repos/$owner/$repo")
                    .header("Authorization", "Bearer $pat")
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .build()
                try {
                    OkHttpClient().newCall(request).execute().use { it.isSuccessful }
                } catch (e: Exception) {
                    false
                }
            }
    }
}
