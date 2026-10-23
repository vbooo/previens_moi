package fr.previensmoi.data.auth

import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.SessionRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RemoteSessionRepository(private val api: AuthApi, private val vault: SessionVault, private val clock: Clock, private val beforeSignOut: suspend (String) -> Unit = {}, private val deleteAccount: suspend (String) -> Outcome<Unit> = { Outcome.Error(Failure.Unsupported) }) : SessionRepository {
    private val mutex = Mutex()
    private var credentials = try { vault.read() } catch (_: Exception) { vault.clear(); null }
    private val session = MutableStateFlow(credentials?.let { Session(it.userId,it.email) })
    fun currentUserId(): String? = session.value?.id
    override fun observeSession() = session.asStateFlow()
    // A real session always requires the explicit email + OTP flow.
    override suspend fun signIn(): Outcome<Session> = Outcome.Error(Failure.SessionRequired)
    override suspend fun requestCode(email: String): Outcome<Unit> = safe {
        if (email.length !in 3..254 || !email.contains('@')) return@safe Outcome.Error(Failure.InvalidInput)
        val response = api.request(OtpRequest(email))
        if (response.isSuccessful) Outcome.Success(Unit) else Outcome.Error(if (response.code() == 429) Failure.QuotaExceeded else Failure.TemporaryFailure)
    }
    override suspend fun verifyCode(email: String, code: String): Outcome<Session> = mutex.withLock { safe {
        if (code.length != 6 || code.any { !it.isDigit() }) return@safe Outcome.Error(Failure.InvalidInput)
        val response = api.verify(VerifyRequest(email, code))
        val value = response.body()
        if (!response.isSuccessful || value == null) Outcome.Error(Failure.SessionRequired)
        else { save(value); Outcome.Success(Session(value.user.id,value.user.email)) }
    } }
    suspend fun bearer(rejected: String? = null): String? = mutex.withLock {
        val current = credentials ?: return@withLock null
        if ((rejected == null || rejected != "Bearer " + current.accessToken) && current.expiresAt > clock.now() + 30_000) return@withLock "Bearer " + current.accessToken
        try {
            val response = api.refresh(RefreshRequest(current.refreshToken))
            val value = response.body()
            if (response.isSuccessful && value != null) { save(value); "Bearer " + value.access_token }
            else { if (response.code() in setOf(400,401,403)) clear(); null }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: IOException) { null }
    }
    private suspend fun save(value: AuthResponse) {
        val next = Credentials(value.user.id, value.access_token, value.refresh_token, clock.now() + value.expires_in * 1000,value.user.email)
        withContext(Dispatchers.IO) { vault.write(next) }
        credentials = next; session.value = Session(next.userId,next.email)
    }
    suspend fun invalidate() = mutex.withLock { clear() }
    private suspend fun clear() { withContext(Dispatchers.IO) { vault.clear() }; credentials = null; session.value = null }
    override suspend fun requestDeletion(): Outcome<Unit> = mutex.withLock {
        val current = credentials ?: return@withLock Outcome.Error(Failure.SessionRequired)
        val result = safe { deleteAccount("Bearer " + current.accessToken) }
        if (result is Outcome.Success) clear()
        result
    }
    override suspend fun signOut(): Outcome<Unit> = mutex.withLock {
        val current = credentials
        try { if (current != null) { beforeSignOut("Bearer " + current.accessToken); api.logout("Bearer " + current.accessToken) } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: IOException) { /* local erasure is still required */ }
        finally { withContext(NonCancellable) { clear() } }
        Outcome.Success(Unit)
    }
    private suspend fun <T> safe(block: suspend () -> Outcome<T>): Outcome<T> = try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: IOException) { Outcome.Error(Failure.TemporaryFailure) }
        catch (_: kotlinx.serialization.SerializationException) { Outcome.Error(Failure.TemporaryFailure) }
        catch (_: java.security.GeneralSecurityException) { Outcome.Error(Failure.StorageFailure) }
}
