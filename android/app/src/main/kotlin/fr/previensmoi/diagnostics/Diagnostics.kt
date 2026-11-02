package fr.previensmoi.diagnostics

import android.util.Log
import fr.previensmoi.BuildConfig

/** Debug-only metadata. Keep free-form text and exception messages outside this API. */
object Diagnostics {
    enum class Event { Http, WatchRefresh, Command, Finding, DraftReset, Session }
    enum class Result { Success, Rejected, NetworkFailure, InvalidResponse, Cancelled, Started }
    internal fun format(event: Event, result: Result, elapsedMs: Long = 0, status: Int? = null, requestId: String? = null): String {
        val safeId = requestId?.takeIf { it.matches(Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) }
        return "event=$event result=$result durationMs=${elapsedMs.coerceAtLeast(0)}" +
            (status?.takeIf { it in 100..599 }?.let { " status=$it" } ?: "") +
            (safeId?.let { " requestId=$it" } ?: "")
    }
    fun record(event: Event, result: Result, elapsedMs: Long = 0, status: Int? = null, requestId: String? = null) {
        if (BuildConfig.DEBUG) runCatching { Log.d("PreviensMoi", format(event, result, elapsedMs, status, requestId)) }
    }
}
