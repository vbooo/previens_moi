package fr.previensmoi.data.push

import fr.previensmoi.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class PreparationFindingTest {
    private val plan = Proposal("p", 2, RequestDraft("Android Montpellier"), Long.MAX_VALUE)
    private val watch = Watch("w", plan, status = WatchStatus.Completed, baselineAt = 1)
    private val initial = Finding("f", "w", "p", 2, "Android Montpellier", 10, fact = "Offres Android à Montpellier", noveltyBasis = "initial")

    @Test fun `first result on completed once watch opens the finding instead of ready notice`() {
        assertEquals(initial, preparationFinding(watch, listOf(initial)))
    }
    @Test fun `continuous watch also announces a first result`() {
        assertEquals(initial, preparationFinding(watch.copy(status = WatchStatus.Active), listOf(initial)))
    }
    @Test fun `ready watch without initial result keeps a ready notice`() {
        assertNull(preparationFinding(watch.copy(status = WatchStatus.Active), emptyList()))
        assertNull(preparationFinding(watch, listOf(initial.copy(noveltyBasis = "baseline_change"))))
    }
    @Test fun `findings from another watch plan or plan version cannot be shown`() {
        assertNull(preparationFinding(watch, listOf(initial.copy(watchId = "other"), initial.copy(planId = "old"), initial.copy(planVersion = 1))))
    }
    @Test fun `failed or paused preparation cannot announce old success`() {
        assertNull(preparationFinding(watch.copy(status = WatchStatus.Paused, health = Health.Degraded), listOf(initial)))
        assertNull(preparationFinding(watch.copy(status = WatchStatus.Pending), listOf(initial)))
    }
}
