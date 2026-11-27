package fr.previensmoi.data.remote

import retrofit2.Response
import retrofit2.http.*

// Credentials are explicit per operation. No interceptor sends a preview secret or JWT
// to every request, and redirect following is disabled by the factory.
interface ApplicationApi {
    @POST("public/v1/support") suspend fun support(@Body request: SupportRequestDto): Response<SupportReceiptDto>
    @GET("api/v1/subscription") suspend fun subscription(@Header("Authorization") bearer: String): Response<SubscriptionAccessDto>
    @GET("api/v1/subscription?verify=true") suspend fun checkSubscription(@Header("Authorization") bearer: String): Response<SubscriptionAccessDto>
    @POST("api/v1/subscription/purchases") suspend fun verifyPurchase(@Header("Authorization") bearer: String, @Body request: PurchaseVerificationDto): Response<SubscriptionAccessDto>
    @GET("public/v1/health") suspend fun health(): Response<HealthDto>
    @POST("public/v1/preview-sessions") suspend fun previewSession(): Response<PreviewSessionDto>
    @POST("public/v1/understandings") suspend fun evaluate(@Header("X-Preview-Token") token: String, @Body request: EvaluationRequestDto, @Header("Authorization") bearer: String? = null): Response<EvaluationDto>
    @GET("public/v1/evaluations/{id}") suspend fun evaluation(@Header("X-Preview-Token") token: String, @Path("id") id: String, @Header("Authorization") bearer: String? = null): Response<EvaluationDto>
    @GET("public/v1/proposals/{id}") suspend fun proposal(@Header("X-Preview-Token") token: String, @Path("id") id: String, @Header("Authorization") bearer: String? = null): Response<ProposalDto>
    @PUT("public/v1/proposals/{id}/options") suspend fun proposalOptions(@Header("X-Preview-Token") token: String, @Path("id") id: String, @Body options: ProposalOptionsDto, @Header("Authorization") bearer: String? = null): Response<ProposalDto>
    @POST("api/v1/commands") suspend fun command(@Header("Authorization") bearer: String, @Body request: CommandDto): Response<CommandReceiptDto>
    @GET("api/v1/commands/{id}") suspend fun commandReceipt(@Header("Authorization") bearer: String, @Path("id") id: String): Response<CommandReceiptDto>
    @GET("api/v1/watches") suspend fun watches(@Header("Authorization") bearer: String, @Query("cursor") cursor: String? = null, @Query("limit") limit: Int = 20): Response<WatchPageDto>
    @GET("api/v1/watches/{id}") suspend fun watch(@Header("Authorization") bearer: String, @Path("id") id: String): Response<WatchDto>
    @GET("api/v1/watches/{id}/findings") suspend fun findings(@Header("Authorization") bearer: String, @Path("id") id: String, @Query("cursor") cursor: String? = null, @Query("limit") limit: Int = 20): Response<FindingPageDto>
    @GET("api/v1/findings/{id}") suspend fun finding(@Header("Authorization") bearer: String, @Path("id") id: String): Response<FindingDto>
    @GET("api/v1/account") suspend fun account(@Header("Authorization") bearer: String): Response<AccountDto>
    @POST("api/v1/account/deletion") suspend fun deleteAccount(@Header("Authorization") bearer: String, @Body request: DeletionRequestDto): Response<DeletionReceiptDto>
    @PUT("api/v1/devices/{id}") suspend fun registerDevice(@Header("Authorization") bearer: String, @Path("id") id: String, @Body request: DeviceRegistrationDto): Response<Unit>
    @DELETE("api/v1/devices/{id}") suspend fun removeDevice(@Header("Authorization") bearer: String, @Path("id") id: String): Response<Unit>
}
