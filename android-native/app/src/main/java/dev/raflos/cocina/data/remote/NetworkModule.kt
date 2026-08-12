package dev.raflos.cocina.data.remote

import dev.raflos.cocina.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Route
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class NetworkModule(private val tokenStore: TokenStore) {

    private val jsonMediaType = "application/json".toMediaType()

    /** Synchronous refresh call, used inside the Authenticator (which runs off the main thread). */
    private fun refreshSync(client: OkHttpClient, refreshToken: String): String? {
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
        val request = Request.Builder()
            .url("${BuildConfig.API_URL}/auth/refresh")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val text = resp.body?.string() ?: return null
                json.decodeFromString(RefreshResponse.serializer(), text).accessToken
            }
        } catch (e: Exception) {
            null
        }
    }

    private val plainClient = OkHttpClient.Builder().build()

    private val authenticator = Authenticator { _: Route?, response: okhttp3.Response ->
        if (responseCount(response) >= 2) return@Authenticator null
        val refreshToken = tokenStore.getRefresh() ?: return@Authenticator null
        val newAccess = refreshSync(plainClient, refreshToken) ?: run {
            tokenStore.clear()
            return@Authenticator null
        }
        tokenStore.setAccess(newAccess)
        response.request.newBuilder().header("Authorization", "Bearer $newAccess").build()
    }

    private fun responseCount(response: okhttp3.Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }

    private val authHeaderInterceptor = Interceptor { chain ->
        val access = tokenStore.getAccess()
        val request = if (access != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $access").build()
        } else chain.request()
        chain.proceed(request)
    }

    private val authedClient = plainClient.newBuilder()
        .addInterceptor(authHeaderInterceptor)
        .authenticator(authenticator)
        .build()

    val authApi: AuthApi = Retrofit.Builder()
        .baseUrl("${BuildConfig.API_URL}/")
        .client(plainClient)
        .addConverterFactory(json.asConverterFactory(jsonMediaType))
        .build()
        .create(AuthApi::class.java)

    val cocinaApi: CocinaApi = Retrofit.Builder()
        .baseUrl("${BuildConfig.API_URL}/")
        .client(authedClient)
        .addConverterFactory(json.asConverterFactory(jsonMediaType))
        .build()
        .create(CocinaApi::class.java)

    fun parseConflict(body: String?): StateConflict? =
        body?.let { runCatching { json.decodeFromString(StateConflict.serializer(), it) }.getOrNull() }
}
