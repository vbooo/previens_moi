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
    @Provides @Singleton fun installation(@ApplicationContext context: Context) = DeviceInstallation(context)
    @Provides @Singleton fun sessions(api: AuthApi, vault: SessionVault, clock: Clock, application: ApplicationApi, installation: DeviceInstallation, ids: IdGenerator) = RemoteSessionRepository(api, vault, clock,
        beforeSignOut = { bearer -> application.removeDevice(bearer,installation.id) },
        deleteAccount = { bearer ->
            when (val result = apiCall { application.deleteAccount(bearer,DeletionRequestDto(ids.next())) }) {
                is ApiResult.Value -> if (result.body.state == "accepted") Outcome.Success(Unit) else Outcome.Error(Failure.TemporaryFailure)
                is ApiResult.Rejected -> Outcome.Error(if (result.httpStatus == 401) Failure.SessionRequired else Failure.TemporaryFailure)
                else -> Outcome.Error(Failure.TemporaryFailure)
            }
        })
    @Provides fun session(repository: RemoteSessionRepository): SessionRepository = repository
    @Provides @Singleton fun journal(@ApplicationContext context: Context): PendingCommands = FilePendingCommands(context)
    @Provides @Singleton fun repositories(api: ApplicationApi, session: RemoteSessionRepository, journal: PendingCommands, ids: IdGenerator) = RemoteRepositories(api,session,journal,ids)
    @Provides fun watches(repository: RemoteRepositories): WatchRepository = repository
    @Provides fun proposals(repository: RemoteRepositories): ProposalRepository = repository
    @Provides fun findings(repository: RemoteRepositories): FindingRepository = repository
    @Provides fun drafts(repository: LocalDraftRepository): DraftRepository = repository
}
