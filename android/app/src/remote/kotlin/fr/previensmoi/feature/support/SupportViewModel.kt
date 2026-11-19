package fr.previensmoi.feature.support

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.previensmoi.domain.model.Failure
import fr.previensmoi.domain.model.Outcome
import fr.previensmoi.domain.repository.SupportMessage
import fr.previensmoi.domain.repository.SupportRepository
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SupportProblem {
    Invalid,
    Offline,
    Unavailable,
    Limit,
}

data class SupportState(
    val opened: Boolean = false,
    val category: String = "question",
    val message: String = "",
    val email: String = "",
    val busy: Boolean = false,
    val received: Boolean = false,
    val problem: SupportProblem? = null,
)

@HiltViewModel
class SupportViewModel
@Inject
constructor(private val repository: SupportRepository, private val saved: SavedStateHandle) :
    ViewModel() {
    private val mutable =
        MutableStateFlow(
            SupportState(
                opened = saved["support.open"] ?: false,
                category = saved["support.category"] ?: "question",
                message = saved["support.message"] ?: "",
                email = saved["support.email"] ?: "",
                received = saved["support.received"] ?: false,
            ),
        )
    val state = mutable.asStateFlow()

    fun open() {
        saved["support.open"] = true
        mutable.value = mutable.value.copy(opened = true)
    }

    fun close() {
        saved["support.open"] = false
        mutable.value = mutable.value.copy(opened = false)
    }

    private fun changed(next: SupportState) {
        if (mutable.value.busy) return
        val current = mutable.value
        if (
            next.category == current.category &&
                next.message == current.message &&
                next.email == current.email
        )
            return
        saved["support.id"] = null
        saved["support.language"] = null
        saved["support.version"] = null
        saved["support.category"] = next.category
        saved["support.message"] = next.message
        saved["support.email"] = next.email
        mutable.value = next.copy(problem = null)
    }

    fun message(value: String) = changed(mutable.value.copy(message = value.take(2000)))

    fun email(value: String) = changed(mutable.value.copy(email = value.take(254)))

    fun category(value: String) {
        if (value in listOf("question", "problem", "idea"))
            changed(mutable.value.copy(category = value))
    }

    fun done() {
        saved["support.id"] = null
        saved["support.language"] = null
        saved["support.version"] = null
        saved["support.message"] = ""
        saved["support.email"] = ""
        saved["support.category"] = "question"
        saved["support.received"] = false
        mutable.value = SupportState()
        close()
    }

    fun send(version: String) {
        val current = mutable.value
        if (current.busy || current.received) return
        val email = current.email.trim()
        if (
            current.message.isBlank() ||
                (email.isNotEmpty() && !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email))
        ) {
            mutable.value = current.copy(problem = SupportProblem.Invalid)
            return
        }
        val id =
            saved.get<String>("support.id")
                ?: UUID.randomUUID().toString().also { saved["support.id"] = it }
        val language =
            saved.get<String>("support.language")
                ?: (if (Locale.getDefault().language == "fr") "fr" else "en").also {
                    saved["support.language"] = it
                }
        val appVersion =
            saved.get<String>("support.version") ?: version.also { saved["support.version"] = it }
        val request =
            SupportMessage(
                id,
                current.category,
                current.message.trim(),
                email.ifEmpty { null },
                appVersion,
                language,
            )
        mutable.value = current.copy(busy = true, problem = null)
        viewModelScope.launch {
            val problem =
                try {
                    when (val result = repository.submit(request)) {
                        is Outcome.Success -> null
                        is Outcome.Error ->
                            when (result.reason) {
                                Failure.Offline -> SupportProblem.Offline
                                Failure.QuotaExceeded -> SupportProblem.Limit
                                Failure.InvalidInput -> SupportProblem.Invalid
                                else -> SupportProblem.Unavailable
                            }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: java.io.IOException) {
                    SupportProblem.Offline
                } catch (_: Exception) {
                    SupportProblem.Unavailable
                }
            saved["support.received"] = problem == null
            if (problem == null) {
                saved["support.message"] = ""
                saved["support.email"] = ""
            }
            mutable.value =
                mutable.value.copy(
                    busy = false,
                    received = problem == null,
                    problem = problem,
                    message = if (problem == null) "" else mutable.value.message,
                    email = if (problem == null) "" else mutable.value.email,
                )
        }
    }
}
