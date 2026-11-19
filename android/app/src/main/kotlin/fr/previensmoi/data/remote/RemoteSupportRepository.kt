package fr.previensmoi.data.remote

import fr.previensmoi.domain.model.Failure
import fr.previensmoi.domain.model.Outcome
import fr.previensmoi.domain.repository.SupportMessage
import fr.previensmoi.domain.repository.SupportRepository

class RemoteSupportRepository(private val api: ApplicationApi) : SupportRepository {
    override suspend fun submit(message: SupportMessage): Outcome<Unit> {
        val request =
            with(message) {
                SupportRequestDto(id, category, this.message, replyEmail, appVersion, language)
            }
        return when (val result = apiCall { api.support(request) }) {
            is ApiResult.Value ->
                if (result.body.id == message.id && result.body.state == "received")
                    Outcome.Success(Unit)
                else Outcome.Error(Failure.TemporaryFailure)
            is ApiResult.Rejected ->
                Outcome.Error(
                    when (result.httpStatus) {
                        400 -> Failure.InvalidInput
                        429 -> Failure.QuotaExceeded
                        else -> Failure.TemporaryFailure
                    },
                )
            ApiResult.ConnectionFailure -> Outcome.Error(Failure.Offline)
            else -> Outcome.Error(Failure.TemporaryFailure)
        }
    }
}
