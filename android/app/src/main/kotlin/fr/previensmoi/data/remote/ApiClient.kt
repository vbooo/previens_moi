package fr.previensmoi.data.remote

import java.io.IOException
import fr.previensmoi.diagnostics.Diagnostics
import fr.previensmoi.diagnostics.Diagnostics.Event
import fr.previensmoi.diagnostics.Diagnostics.Result
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ApiClient {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = true }

    fun create(baseUrl: HttpUrl, allowLocalHttp: Boolean = false): ApplicationApi {
        require(baseUrl.isHttps || (allowLocalHttp && baseUrl.host in setOf("localhost", "127.0.0.1", "::1", "10.0.2.2"))) {
            "HTTPS required outside explicitly enabled local testing"
        }
        require(baseUrl.username.isEmpty() && baseUrl.password.isEmpty() && baseUrl.query == null && baseUrl.fragment == null)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
            .build()
        return Retrofit.Builder().baseUrl(baseUrl).client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(ApplicationApi::class.java)
    }
}

// Transport states do not become domain/UI states until the repository maps them.
// In particular a 202 and a CommandReceipt.Pending must not become "active".
sealed interface ApiResult<out T> {
    data class Value<T>(val body: T, val httpStatus: Int): ApiResult<T>
    data object Empty: ApiResult<Nothing>
    data class Rejected(val httpStatus: Int, val error: ErrorDto?): ApiResult<Nothing>
    data object ConnectionFailure: ApiResult<Nothing>
    data object InvalidResponse: ApiResult<Nothing>
}

suspend fun <T> apiCall(call: suspend () -> Response<T>): ApiResult<T> {
    val started = System.nanoTime()
    var requestId: String? = null
    var status: Int? = null
    val result = try {
        val response = call()
        status = response.code()
        requestId = response.headers()["X-Request-Id"]
        if (response.isSuccessful) {
            val body = response.body()
            if (response.code() == 204) ApiResult.Empty
            else if (body == null) ApiResult.InvalidResponse
            else ApiResult.Value(body, response.code())
        } else {
            val error = response.errorBody()?.use { body ->
                // Bound the diagnostic read; never log provider bodies or authorization values.
                val source = body.source()
                source.request(16_385)
                if (source.buffer.size > 16_384) null
                else try { ApiClient.json.decodeFromString<ErrorDto>(source.readUtf8()) }
                catch (_: SerializationException) { null }
                catch (_: IllegalArgumentException) { null }
            }
            requestId = error?.requestId ?: requestId
            ApiResult.Rejected(response.code(), error)
        }
    } catch (cancelled: CancellationException) {
        Diagnostics.record(Event.Http, Result.Cancelled, (System.nanoTime() - started) / 1_000_000)
        throw cancelled
    } catch (_: SerializationException) {
        ApiResult.InvalidResponse
    } catch (_: IllegalArgumentException) {
        ApiResult.InvalidResponse
    } catch (_: IOException) {
        // A transport failure cannot distinguish airplane mode from an unreachable server.
        ApiResult.ConnectionFailure
    }

    Diagnostics.record(Event.Http, when (result) {
        is ApiResult.Value, ApiResult.Empty -> Result.Success
        is ApiResult.Rejected -> Result.Rejected
        ApiResult.ConnectionFailure -> Result.NetworkFailure
        ApiResult.InvalidResponse -> Result.InvalidResponse
    }, (System.nanoTime() - started) / 1_000_000, status, requestId)
    return result
}
