package fr.previensmoi.domain.repository

import fr.previensmoi.domain.model.*
import kotlinx.coroutines.flow.Flow

interface WatchRepository {
    fun observeList(): Flow<List<Watch>>
    suspend fun refresh(): RefreshResult
    suspend fun execute(command: Command): Outcome<CommandReceipt>
}
interface ProposalRepository {
    suspend fun evaluate(draft: RequestDraft): Outcome<Evaluation>
    suspend fun get(id: String): Outcome<Proposal>
    suspend fun configure(proposal: Proposal, mode: FollowMode): Outcome<Proposal> = Outcome.Error(Failure.Unsupported)
}
interface FindingRepository {
    fun observeFindings(): Flow<List<Finding>>
    suspend fun getFinding(id: String): Outcome<Finding>
}
interface SessionRepository {
    fun observeSession(): Flow<Session?>
    suspend fun signIn(): Outcome<Session>
    suspend fun requestCode(email: String): Outcome<Unit> = Outcome.Error(Failure.Unsupported)
    suspend fun verifyCode(email: String, code: String): Outcome<Session> = Outcome.Error(Failure.Unsupported)
    suspend fun signInWithPassword(email: String, password: String): Outcome<Session> = Outcome.Error(Failure.Unsupported)
    suspend fun requestDeletion(): Outcome<Unit> = Outcome.Error(Failure.Unsupported)
    suspend fun signOut(): Outcome<Unit> = Outcome.Error(Failure.Unsupported)
}
interface DraftRepository {
    suspend fun load(): Outcome<RequestDraft>
    suspend fun save(draft: RequestDraft): Outcome<Unit>
    suspend fun clear(): Outcome<Unit>
}
