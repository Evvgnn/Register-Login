package com.example.registerlogin.data.api

import android.util.Log
import com.example.registerlogin.data.models.AuthResponse
import com.example.registerlogin.data.models.LoginRequest
import com.example.registerlogin.data.models.SignUpRequest
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class KtorApiClient private constructor() {

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }

    fun close() {
        try {
            httpClient.close()
        } catch (_: Exception) { /* ignore */ }
    }

    companion object {
        private const val BASE_URL = "https://api--quiz--d7xc6gwzfsnz.code.run"

        @Volatile
        private var INSTANCE: KtorApiClient? = null

        fun getInstance(): KtorApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KtorApiClient().also { INSTANCE = it }
            }
        }

        private fun getErrorMessage(statusCode: Int): String {
            return when (statusCode) {
                400 -> "Error 400: Bad Request"
                401 -> "Error 401: Unauthorized"
                403 -> "Error 403: Forbidden"
                404 -> "Error 404: Not found"
                503 -> "Error 503: Service Unavailable"
                504 -> "Error 504: Gateway Timeout"
                else -> "Unknown error: HTTP $statusCode"
            }
        }
    }

    suspend fun signUp(request: SignUpRequest): ApiResult<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.post("$BASE_URL/signUp") {
                setBody(request)
            }

            if (response.status.isSuccess()) {
                val auth = response.body<AuthResponse>()
                ApiResult.Success(auth)
            } else {
                val bodyText = runCatching { response.bodyAsText() }.getOrNull()
                val message = bodyText?.takeIf { it.isNotBlank() } ?: getErrorMessage(response.status.value)
                ApiResult.Error(response.status.value, message)
            }
        } catch (e: Exception) {
            Log.e("KtorApiClient", "SignUp exception: ${e::class.simpleName}: ${e.message}", e)
            ApiResult.Error(0, "Network error: ${e.message ?: "Unknown error"}")
        }
    }

    suspend fun login(request: LoginRequest): ApiResult<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.post("$BASE_URL/login") {
                setBody(request)
            }

            Log.d("KtorApiClient", "Login response status: ${response.status}")

            if (response.status.isSuccess()) {
                val auth = response.body<AuthResponse>()
                ApiResult.Success(auth)
            } else {
                val bodyText = runCatching { response.bodyAsText() }.getOrNull()
                val message = bodyText?.takeIf { it.isNotBlank() } ?: getErrorMessage(response.status.value)
                Log.d("KtorApiClient", "Login error: status=${response.status.value}, body='$bodyText'")
                ApiResult.Error(response.status.value, message)
            }
        } catch (e: Exception) {
            Log.e("KtorApiClient", "Login exception: ${e::class.simpleName}: ${e.message}", e)
            ApiResult.Error(0, "Network error: ${e.message ?: "Unknown error"}")
        }
    }

    suspend fun refreshToken(email: String, refreshToken: String): ApiResult<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.post("$BASE_URL/refresh") {
                parameter("email", email)
                bearerAuth(refreshToken)
            }

            if (response.status.isSuccess()) {
                val auth = response.body<AuthResponse>()
                ApiResult.Success(auth)
            } else {
                val bodyText = runCatching { response.bodyAsText() }.getOrNull()
                val message = bodyText?.takeIf { it.isNotBlank() } ?: getErrorMessage(response.status.value)
                ApiResult.Error(response.status.value, message)
            }
        } catch (e: Exception) {
            Log.e("KtorApiClient", "RefreshToken exception: ${e::class.simpleName}: ${e.message}", e)
            ApiResult.Error(0, "Network error: ${e.message ?: "Unknown error"}")
        }
    }

    suspend fun getExamAndroid(accessToken: String): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get("$BASE_URL/exam/android") {
                bearerAuth(accessToken)
            }

            if (response.status.isSuccess()) {
                val text = response.bodyAsText()
                ApiResult.Success(text)
            } else {
                val bodyText = runCatching { response.bodyAsText() }.getOrNull()
                val message = bodyText?.takeIf { it.isNotBlank() } ?: getErrorMessage(response.status.value)
                ApiResult.Error(response.status.value, message)
            }
        } catch (e: Exception) {
            Log.e("KtorApiClient", "GetExamAndroid exception: ${e::class.simpleName}: ${e.message}", e)
            ApiResult.Error(0, "Network error: ${e.message ?: "Unknown error"}")
        }
    }

    sealed class ApiResult<out T> {
        data class Success<T>(val data: T) : ApiResult<T>()
        data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    }
}