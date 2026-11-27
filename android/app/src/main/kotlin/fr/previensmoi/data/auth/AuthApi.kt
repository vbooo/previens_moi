package fr.previensmoi.data.auth

import fr.previensmoi.data.remote.ApiClient
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*

@Serializable class OtpRequest(val email: String, val create_user: Boolean = true) {
    override fun toString() = "OtpRequest([redacted])"
}
@Serializable class VerifyRequest(val email: String, val token: String, val type: String = "email") {
    override fun toString() = "VerifyRequest([redacted])"
}
@Serializable class PasswordRequest(val email: String, val password: String) { override fun toString() = "PasswordRequest([redacted])" }
@Serializable class RefreshRequest(val refresh_token: String) { override fun toString() = "RefreshRequest([redacted])" }
@Serializable data class AuthUser(val id: String, val email: String = "")
@Serializable class AuthResponse(val access_token: String, val refresh_token: String, val expires_in: Long, val user: AuthUser) {
    override fun toString() = "AuthResponse([redacted])"
}
interface AuthApi {
    @POST("token?grant_type=password") suspend fun password(@Body request: PasswordRequest): Response<AuthResponse>
    @POST("otp") suspend fun request(@Body request: OtpRequest): Response<Unit>
    @POST("verify") suspend fun verify(@Body request: VerifyRequest): Response<AuthResponse>
    @POST("token?grant_type=refresh_token") suspend fun refresh(@Body request: RefreshRequest): Response<AuthResponse>
    @POST("logout?scope=local") suspend fun logout(@Header("Authorization") bearer: String): Response<Unit>
}
fun createAuthApi(url: HttpUrl, publicKey: String, local: Boolean = false): AuthApi {
    require(url.isHttps || (local && url.host in setOf("127.0.0.1", "localhost", "10.0.2.2")))
    require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null)
    val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("apikey", publicKey).build()) }.build()
    return Retrofit.Builder().baseUrl(url).client(client)
        .addConverterFactory(ApiClient.json.asConverterFactory("application/json".toMediaType())).build().create(AuthApi::class.java)
}
