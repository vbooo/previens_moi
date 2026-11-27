package fr.previensmoi.app

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.previensmoi.data.auth.*
import fr.previensmoi.data.local.*
import fr.previensmoi.data.remote.*
import fr.previensmoi.domain.model.Clock
import fr.previensmoi.domain.model.IdGenerator
import fr.previensmoi.domain.model.Outcome
import fr.previensmoi.domain.model.Failure
import fr.previensmoi.domain.repository.*
import fr.previensmoi.data.push.DeviceInstallation
import fr.previensmoi.BuildConfig
import java.security.SecureRandom
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object RemoteModule {
    @Provides @Singleton fun support(api: ApplicationApi): SupportRepository = RemoteSupportRepository(api)
    @Provides @Singleton fun playBilling(@ApplicationContext context: Context) = fr.previensmoi.data.billing.GooglePlayBilling(context)
    @Provides fun storeBilling(value: fr.previensmoi.data.billing.GooglePlayBilling): StoreBilling = value
    @Provides @Singleton fun subscriptionCache(@ApplicationContext context: Context): fr.previensmoi.data.billing.SubscriptionCache = fr.previensmoi.data.billing.KeystoreSubscriptionCache(context)
    @Provides @Singleton fun subscriptions(api: ApplicationApi, session: RemoteSessionRepository, cache: fr.previensmoi.data.billing.SubscriptionCache, clock: Clock): SubscriptionRepository = fr.previensmoi.data.billing.RemoteSubscriptionRepository(api,session,cache,clock)
    @Provides fun clock(): Clock = Clock { System.currentTimeMillis() }
    @Provides @Singleton fun ids(): IdGenerator = IdGenerator {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }; val now = System.currentTimeMillis()
        for (i in 0..5) bytes[5-i] = (now ushr (8*i)).toByte()
        bytes[6] = ((bytes[6].toInt() and 15) or 0x70).toByte(); bytes[8] = ((bytes[8].toInt() and 63) or 0x80).toByte()
        val b = java.nio.ByteBuffer.wrap(bytes); java.util.UUID(b.long,b.long).toString()
    }
    @Provides @Singleton fun auth(): AuthApi = createAuthApi(BuildConfig.AUTH_URL.toHttpUrl(), BuildConfig.AUTH_PUBLIC_KEY, BuildConfig.LOCAL_SERVICE)
    @Provides @Singleton fun api(): ApplicationApi = ApiClient.create(BuildConfig.SERVICE_URL.toHttpUrl(), BuildConfig.LOCAL_SERVICE)
    @Provides @Singleton fun vault(@ApplicationContext context: Context): SessionVault = KeystoreSessionVault(context)
    @Provides @Singleton fun notificationClaims(@ApplicationContext context: Context) = fr.previensmoi.data.push.NotificationClaims(context)
    @Provides @Singleton fun installation(@ApplicationContext context: Context) = DeviceInstallation(context)
    @Provides @Singleton fun sessions(api: AuthApi, vault: SessionVault, clock: Clock, application: ApplicationApi, installation: DeviceInstallation, ids: IdGenerator, cache: fr.previensmoi.data.billing.SubscriptionCache) = RemoteSessionRepository(api, vault, clock,
        beforeSignOut = { bearer -> kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { cache.clear() } }; application.removeDevice(bearer,installation.id) },
        deleteAccount = { bearer ->
            when (val result = apiCall { application.deleteAccount(bearer,DeletionRequestDto(ids.next())) }) {
                is ApiResult.Value -> if (result.body.state == "accepted") { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { cache.clear() } }; Outcome.Success(Unit) } else Outcome.Error(Failure.TemporaryFailure)
                is ApiResult.Rejected -> Outcome.Error(if (result.httpStatus == 401) Failure.SessionRequired else Failure.TemporaryFailure)
                else -> Outcome.Error(Failure.TemporaryFailure)
            }
        })
    @Provides fun session(repository: RemoteSessionRepository): SessionRepository = repository
    @Provides @Singleton fun journal(@ApplicationContext context: Context): PendingCommands = FilePendingCommands(context)
    @Provides @Singleton fun evaluationJournal(@ApplicationContext context: Context): EvaluationJournal = fr.previensmoi.data.local.KeystoreEvaluationJournal(context)
    @Provides @Singleton fun repositories(evaluations: EvaluationJournal, api: ApplicationApi, session: RemoteSessionRepository, journal: PendingCommands, ids: IdGenerator) = RemoteRepositories(api,session,journal,ids,evaluations)
    @Provides fun watches(repository: RemoteRepositories): WatchRepository = repository
    @Provides fun proposals(repository: RemoteRepositories): ProposalRepository = repository
    @Provides fun findings(repository: RemoteRepositories): FindingRepository = repository
    @Provides fun drafts(repository: LocalDraftRepository): DraftRepository = repository
}
