package fr.previensmoi.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.DraftRepository
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.draftStore by preferencesDataStore(name = "creation_draft")

@Singleton
class LocalDraftRepository @Inject constructor(@param:ApplicationContext private val context: Context) : DraftRepository {
    private val text = stringPreferencesKey("text")
    private val answer = stringPreferencesKey("answer")
    private val language = stringPreferencesKey("language")
    private val mode = stringPreferencesKey("mode")
    private val revision = longPreferencesKey("revision")
    override suspend fun load(): Outcome<RequestDraft> = try {
        val p = context.draftStore.data.first()
        Outcome.Success(RequestDraft(p[text].orEmpty(), p[answer].orEmpty(), p[language] ?: "fr",
            FollowMode.entries.firstOrNull { it.name == p[mode] } ?: FollowMode.Once, p[revision] ?: 0))
    } catch (_: IOException) { Outcome.Error(Failure.StorageFailure) }
    override suspend fun save(draft: RequestDraft): Outcome<Unit> = try {
        context.draftStore.edit { p ->
            p[text] = draft.text; p[answer] = draft.answer; p[language] = draft.language
            p[mode] = draft.mode.name; p[revision] = draft.revision
        }
        Outcome.Success(Unit)
    } catch (_: IOException) { Outcome.Error(Failure.StorageFailure) }
    override suspend fun clear(): Outcome<Unit> = try {
        context.draftStore.edit { it.clear() }
        Outcome.Success(Unit)
    } catch (_: IOException) { Outcome.Error(Failure.StorageFailure) }
}
