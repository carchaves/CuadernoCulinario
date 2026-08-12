package dev.raflos.cocina.data.remote

import dev.raflos.cocina.data.model.AppState
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val accessToken: String, val refreshToken: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class RefreshResponse(val accessToken: String)

@Serializable
data class StateResponse(val data: AppState? = null, val revision: Int = 0, val updatedAt: String? = null)

@Serializable
data class StatePutRequest(val data: AppState, val baseRevision: Int)

@Serializable
data class StatePutResult(val revision: Int = 0, val updatedAt: String? = null)

@Serializable
data class StateConflict(val error: String, val data: AppState? = null, val revision: Int = 0)

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse
}

interface CocinaApi {
    @GET("api/state")
    suspend fun getState(): StateResponse

    @PUT("api/state")
    suspend fun putState(@Body body: StatePutRequest): Response<StatePutResult>
}
