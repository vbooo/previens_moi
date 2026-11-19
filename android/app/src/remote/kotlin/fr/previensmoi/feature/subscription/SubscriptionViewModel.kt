package fr.previensmoi.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SubscriptionUiState(
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val buying: Boolean = false,
    val access: SubscriptionAccess? = null,
    val product: SubscriptionProduct? = null,
    val problem: BillingProblem? = null,
    val restored: Boolean = false,
)

@HiltViewModel
class SubscriptionViewModel
@Inject
constructor(private val repository: SubscriptionRepository, private val store: StoreBilling) :
    ViewModel() {
    private val mutable = MutableStateFlow(SubscriptionUiState())
    val state = mutable.asStateFlow()
    private val mutex = Mutex()
    private var owner: String? = null
    private var epoch = 0
    private var restoreOnLoad = false

    init {
        viewModelScope.launch {
            store.purchases.collect { notice ->
                when (notice) {
                    is StoreNotice.Problem ->
                        if (owner != null && state.value.buying)
                            mutable.update {
                                it.copy(busy = false, buying = false, problem = notice.problem)
                            }
                    is StoreNotice.Purchase -> verify(notice.value)
                }
            }
        }
    }

    fun bindOwner(value: String?) {
        if (value == owner) return
        owner = value
        restoreOnLoad = value != null
        epoch++
        mutable.value = SubscriptionUiState()
        if (value != null) refresh()
    }

    fun refresh() {
        val generation = epoch
        if (owner == null) return
        viewModelScope.launch {
            mutex.withLock {
                if (generation != epoch) return@withLock
                mutable.update { it.copy(busy = true, problem = null, restored = false) }
                when (val access = repository.access()) {
                    is BillingOutcome.Error ->
                        if (generation == epoch)
                            mutable.update {
                                it.copy(
                                    loaded = true,
                                    busy = false,
                                    buying = false,
                                    problem = access.problem,
                                )
                            }
                    is BillingOutcome.Success -> {
                        val product = if (access.value.canPurchase) store.product() else null
                        val recover =
                            generation == epoch &&
                                access.value.canPurchase &&
                                (restoreOnLoad || !access.value.active)
                        // An expired server snapshot can overlap a Play renewal. Reconcile
                        // owned purchases before publishing an offer to subscribe again.
                        val recovered = if (recover) recoverAccess(generation) else null
                        if (generation == epoch) restoreOnLoad = false
                        if (generation == epoch)
                            mutable.update {
                                it.copy(
                                    loaded = true,
                                    busy = false,
                                    buying = false,
                                    access = recovered?.access ?: access.value,
                                    product = (product as? BillingOutcome.Success)?.value,
                                    problem = recovered?.problem,
                                )
                            }
                    }
                }
            }
        }
    }

    fun beginPurchase(): String? {
        val current = state.value
        if (
            owner == null ||
                current.busy ||
                current.access?.canPurchase != true ||
                current.access.active ||
                current.problem !in setOf(null, BillingProblem.Cancelled) ||
                current.access.status !in
                    setOf(SubscriptionStatus.None, SubscriptionStatus.Expired) ||
                current.product == null
        )
            return null
        mutable.update { it.copy(busy = true, buying = true, problem = null, restored = false) }
        return current.access.accountId
    }

    fun launchResult(result: BillingOutcome<Unit>) {
        if (result is BillingOutcome.Error)
            mutable.update { it.copy(busy = false, buying = false, problem = result.problem) }
    }

    fun restore(announce: Boolean = true) {
        val generation = epoch
        if (owner == null) return
        if (!announce && state.value.access?.canPurchase != true) {
            refresh()
            return
        }
        viewModelScope.launch {
            mutex.withLock {
                if (generation != epoch) return@withLock
                mutable.update {
                    it.copy(busy = true, buying = false, problem = null, restored = false)
                }
                val recovered = recoverAccess(generation)
                if (generation == epoch)
                    mutable.update {
                        it.copy(
                            loaded = true,
                            busy = false,
                            access = recovered.access ?: it.access,
                            problem = recovered.problem,
                            restored =
                                announce && recovered.access != null && recovered.problem == null,
                        )
                    }
            }
        }
    }

    fun dismissRestoreMessage() {
        mutable.update { it.copy(restored = false) }
    }

    private data class Recovery(val access: SubscriptionAccess?, val problem: BillingProblem?)

    private suspend fun recoverAccess(generation: Int): Recovery {
        when (val result = store.restore()) {
            is BillingOutcome.Error -> return Recovery(null, result.problem)
            is BillingOutcome.Success -> {
                var error: BillingProblem? = null
                for (purchase in result.value) {
                    if (generation != epoch) return Recovery(null, null)
                    if (purchase.pending) {
                        error = BillingProblem.Pending
                        continue
                    }
                    val checked = repository.verify(purchase.token)
                    if (checked is BillingOutcome.Error) error = checked.problem
                }
                if (generation != epoch) return Recovery(null, null)
                return when (val access = repository.access()) {
                    is BillingOutcome.Success -> Recovery(access.value, error)
                    is BillingOutcome.Error -> Recovery(null, error ?: access.problem)
                }
            }
        }
    }

    private fun verify(purchase: PurchaseNotice) {
        val generation = epoch
        if (owner == null) return
        if (purchase.pending) {
            mutable.update {
                it.copy(busy = false, buying = false, problem = BillingProblem.Pending)
            }
            return
        }
        if (purchase.accountId != null && purchase.accountId != state.value.access?.accountId) {
            mutable.update {
                it.copy(busy = false, buying = false, problem = BillingProblem.WrongAccount)
            }
            return
        }
        viewModelScope.launch {
            mutex.withLock {
                if (generation != epoch) return@withLock
                mutable.update { it.copy(busy = true, buying = false, problem = null) }
                when (val checked = repository.verify(purchase.token)) {
                    is BillingOutcome.Success ->
                        if (generation == epoch)
                            mutable.update {
                                it.copy(loaded = true, busy = false, access = checked.value)
                            }
                    is BillingOutcome.Error ->
                        if (generation == epoch)
                            mutable.update { it.copy(busy = false, problem = checked.problem) }
                }
            }
        }
    }
}
