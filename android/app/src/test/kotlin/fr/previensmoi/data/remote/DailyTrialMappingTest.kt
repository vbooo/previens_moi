package fr.previensmoi.data.remote

import fr.previensmoi.domain.model.Failure
import fr.previensmoi.domain.model.WatchStatus
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class DailyTrialMappingTest {
    private val cases = ApiClient.json.parseToJsonElement(javaClass.getResourceAsStream("/application-v1.examples.json")!!.bufferedReader().use { it.readText() }).jsonArray
    private fun payload(name: String) = cases.first { it.jsonObject["name"]!!.jsonPrimitive.content == name }.jsonObject["payload"].toString()
    @Test fun `trial expiry is readable and retains the existing wire status`() {
        val watch = ApiClient.json.decodeFromString<WatchDto>(payload("ended_free_trial_preserves_watch")).domain()
        assertTrue(watch.trialEnded)
        assertNotNull(watch.trialEndsAt)
        assertEquals(WatchStatus.SuspendedByCapacity, watch.status)
        assertEquals(24, watch.plan.intervalHours)
        assertEquals(7, watch.plan.trialDays)
        assertEquals(1, watch.plan.limit)
    }
    @Test fun `legacy watches do not receive an invented trial deadline`() {
        val old = cases.first { it.jsonObject["schema"]?.jsonPrimitive?.content == "Watch" && it.jsonObject["valid"]!!.jsonPrimitive.boolean }.jsonObject["payload"].toString()
        val watch = ApiClient.json.decodeFromString<WatchDto>(old).domain()
        assertFalse(watch.trialEnded)
        assertNull(watch.trialEndsAt)
        assertNull(watch.plan.trialDays)
    }
    @Test fun `trial end is distinct from temporary shared capacity exhaustion`() {
        assertEquals(Failure.TrialEnded, errorFailure("free_trial_ended"))
        assertEquals(Failure.QuotaExceeded, errorFailure("quota_exceeded"))
        assertEquals(Failure.ActiveLimit, errorFailure("active_watch_limit"))
    }
}
