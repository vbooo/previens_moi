package fr.previensmoi.feature.session.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val session: Session? = null,
    val email: String = "",
    val code: String = "",
    val sent: Boolean = false,
    val busy: Boolean = false,
    val error: Failure? = null,
    val deletionAccepted: Boolean = false,
    val resendSeconds: Int = 0,
    val loaded: Boolean = false,
)

@HiltViewModel
class SessionViewModel @Inject constructor(private val repository: SessionRepository) :
    ViewModel() {
    private val mutable = MutableStateFlow(SessionUiState())
    val state = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSession().collect { s -> mutable.update { it.copy(session = s, loaded = true) } }
        }
    }

    fun email(value: String) {
        if (!state.value.busy)
            mutable.update { it.copy(email = value, sent = false, code = "", error = null) }
    }

    fun code(value: String) {
        if (!state.value.busy)
            mutable.update { it.copy(code = value.filter(Char::isDigit).take(6), error = null) }
    }

    fun changeEmail() {
        if (!state.value.busy) mutable.update { it.copy(sent = false, code = "", error = null) }
    }

    fun send() {
        if (state.value.busy || state.value.resendSeconds > 0) return
        val email = state.value.email.trim()
        mutable.update { it.copy(busy = true, code = "", error = null) }
        viewModelScope.launch {
            val r = repository.requestCode(email)
            mutable.update {
                it.copy(
                    busy = false,
                    sent = r is Outcome.Success || it.sent,
                    error = (r as? Outcome.Error)?.reason,
                )
            }
            if (r is Outcome.Success) {
                mutable.update { it.copy(resendSeconds = 60) }
                repeat(60) {
                    delay(1000)
                    mutable.update {
                        it.copy(resendSeconds = (it.resendSeconds - 1).coerceAtLeast(0))
                    }
                }
            }
        }
    }

    fun verify() {
        if (state.value.busy || !state.value.sent) return
        val email = state.value.email.trim()
        val code = state.value.code
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val r = repository.verifyCode(email, code)
            mutable.update {
                it.copy(
                    busy = false,
                    code = "",
                    email = if (r is Outcome.Success) "" else it.email,
                    error = (r as? Outcome.Error)?.reason,
                )
            }
        }
    }

    fun requestDeletion() {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.requestDeletion()) {
                is Outcome.Success -> mutable.update { SessionUiState(deletionAccepted = true, loaded = true) }
                is Outcome.Error -> mutable.update { it.copy(busy = false, error = result.reason) }
            }
        }
    }

    fun signOut() {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            repository.signOut()
            mutable.update { SessionUiState(loaded = true) }
        }
    }
}
