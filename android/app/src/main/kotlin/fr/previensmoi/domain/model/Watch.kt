package fr.previensmoi.domain.model

/** Minimal S1 snapshot. Plan versions and commands are introduced with their S2 contracts. */
data class Watch(val id: String)
sealed interface RefreshResult {
    data object Success : RefreshResult
    data object TemporaryFailure : RefreshResult
}
