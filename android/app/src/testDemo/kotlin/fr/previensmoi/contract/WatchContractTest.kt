package fr.previensmoi.contract

import fr.previensmoi.data.demo.DemoScenario
import fr.previensmoi.data.demo.DemoService
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** Shared assertions use ports; harness hooks inject faults/time without real sleeps or network. */
abstract class WatchContractTest {
    interface Rig {
        val watches: WatchRepository
        val proposals: ProposalRepository
        val findings: FindingRepository
        val sessions: SessionRepository
        var time: Long
        suspend fun fault(name: String)
        suspend fun check()
    }
    abstract fun createRig(capacity: Int = 1): Rig
    private suspend fun Rig.proposal(text: String = "Synthetic request", mode: FollowMode = FollowMode.Continuous): Proposal {
        val ready = (proposals.evaluate(RequestDraft(text, mode = mode)) as Outcome.Success).value as Evaluation.Ready
        return ready.proposal
    }
    private suspend fun Rig.activate(p: Proposal, id: String = "activate"): CommandReceipt {
        sessions.signIn()
        return (watches.execute(Command(id, Operation.Activate, p.id, p.version)) as Outcome.Success).value
    }
    private fun failure(reason: Failure, actual: Outcome<*>) = assertEquals(Outcome.Error(reason), actual)

    @Test fun C01_expirationIncludesExactBoundary() = runTest {
        val r = createRig(); val p = r.proposal(); r.sessions.signIn(); r.time = p.expiresAt
        failure(Failure.ProposalExpired, r.watches.execute(Command("a", Operation.Activate, p.id, p.version)))
        assertTrue(r.watches.observeList().first().isEmpty())
    }
    @Test fun C02_lostResponseReplayAfterExpiryAndNewIdNeverDuplicates() = runTest {
        val r = createRig(); val p = r.proposal(); r.sessions.signIn(); r.fault("lost")
        val c = Command("a", Operation.Activate, p.id, p.version)
        failure(Failure.TemporaryFailure, r.watches.execute(c))
        val original = r.watches.observeList().first().single()
        r.time = p.expiresAt
        val retry = (r.watches.execute(c) as Outcome.Success).value
        assertEquals(original.id, retry.watchId)
        assertEquals(retry, (r.watches.execute(c) as Outcome.Success).value)
        r.fault("normal")
        val newCommand = (r.watches.execute(c.copy(id = "different")) as Outcome.Success).value
        assertEquals(original.id, newCommand.watchId)
        assertEquals(1, r.watches.observeList().first().size)
    }
    @Test fun C03_commandIdCannotChangeMeaning() = runTest {
        val r = createRig(); val p = r.proposal(); val a = r.activate(p)
        failure(Failure.CommandConflict, r.watches.execute(Command("activate", Operation.Delete, a.watchId, 1)))
        assertEquals(1, r.watches.observeList().first().size)
    }
    @Test fun C04_lastSlotIsAtomic() = runTest {
        val r = createRig(); val a = r.proposal("A"); val b = r.proposal("B"); r.sessions.signIn()
        val outcomes = listOf(a, b).mapIndexed { i, p -> async { r.watches.execute(Command("c$i", Operation.Activate, p.id, p.version)) } }.awaitAll()
        assertEquals(1, outcomes.count { it is Outcome.Success })
        assertEquals(1, outcomes.count { it == Outcome.Error(Failure.QuotaExceeded) })
        assertEquals(1, r.watches.observeList().first().size)
    }
    @Test fun C05_sessionAndVersionAreValidatedBeforeMutation() = runTest {
        val r = createRig(); val p = r.proposal(); val c = Command("a", Operation.Activate, p.id, p.version)
        failure(Failure.SessionRequired, r.watches.execute(c))
        r.sessions.signIn(); failure(Failure.VersionConflict, r.watches.execute(c.copy(expectedVersion = 99)))
        assertTrue(r.watches.observeList().first().isEmpty())
    }
    @Test fun C06_failedPauseAndStaleReplacementPreserveState() = runTest {
        val r = createRig(); val p = r.proposal(); r.activate(p)
        val old = r.watches.observeList().first().single()
        r.fault("commandFailure")
        failure(Failure.TemporaryFailure, r.watches.execute(Command("pause", Operation.Pause, old.id, old.version)))
        assertEquals(old, r.watches.observeList().first().single())
        r.fault("normal"); val next = r.proposal("Replacement")
        failure(Failure.VersionConflict, r.watches.execute(Command("replace", Operation.Replace, old.id, 9, next.id, next.version)))
        assertEquals(old, r.watches.observeList().first().single())
    }
    @Test fun C07_sourceFailureDoesNotReplaceLastSuccess() = runTest {
        val r = createRig(); r.activate(r.proposal()); r.fault("empty"); r.check()
        val success = r.watches.observeList().first().single().lastSuccess
        r.time += 1000; r.fault("sourceFailure"); r.check()
        val failed = r.watches.observeList().first().single()
        assertEquals(success, failed.lastSuccess); assertEquals(Health.Degraded, failed.health)
        r.time += 1000; r.fault("empty"); r.check()
        assertEquals(r.time, r.watches.observeList().first().single().lastSuccess)
        assertTrue(r.findings.observeFindings().first().isEmpty())
    }
    @Test fun C08_deleteReplaysWithoutRevealingDeletedContent() = runTest {
        val r = createRig(); val a = r.activate(r.proposal()); r.check()
        val finding = r.findings.observeFindings().first().single()
        val w = r.watches.observeList().first().single()
        val command = Command("delete", Operation.Delete, a.watchId, w.version)
        val first = r.watches.execute(command)
        assertEquals(first, r.watches.execute(command))
        assertTrue((first as Outcome.Success).value.deleted)
        assertTrue(r.watches.observeList().first().isEmpty())
        assertTrue(r.findings.observeFindings().first().isEmpty())
        failure(Failure.NotFound, r.findings.getFinding(finding.id))
    }
    @Test fun C11_activationWithoutInitialResultIsValid() = runTest {
        val r = createRig(); r.fault("empty"); val p = r.proposal(); assertTrue(p.sources.isEmpty())
        r.activate(p); assertEquals(1, r.watches.observeList().first().size)
        assertTrue(r.findings.observeFindings().first().isEmpty())
    }
    @Test fun C12_resumeDoesNotCatchUpAndOldResultsKeepPlanVersion() = runTest {
        val r = createRig(); r.activate(r.proposal()); r.check()
        val result = r.findings.observeFindings().first().single()
        var watch = r.watches.observeList().first().single()
        r.watches.execute(Command("pause", Operation.Pause, watch.id, watch.version)); r.time += 1000
        watch = r.watches.observeList().first().single(); r.check()
        assertEquals(1, r.findings.observeFindings().first().size)
        r.watches.execute(Command("resume", Operation.Resume, watch.id, watch.version))
        watch = r.watches.observeList().first().single()
        assertEquals(r.time, watch.baselineAt)
        assertEquals(1, r.findings.observeFindings().first().size)
        val replacement = r.proposal("New criteria")
        r.watches.execute(Command("replace", Operation.Replace, watch.id, watch.version, replacement.id, replacement.version))
        assertEquals(result, r.findings.observeFindings().first().single())
        assertNotEquals(result.planId, r.watches.observeList().first().single().plan.id)
    }
    @Test fun capacitySuspensionCannotBeResumedUntilCapacityIsAvailable() = runTest {
        val r = createRig(); r.activate(r.proposal()); r.fault("quota"); r.check()
        val suspended = r.watches.observeList().first().single()
        assertEquals(WatchStatus.SuspendedByCapacity, suspended.status)
        assertNull(suspended.nextCheck)
        failure(Failure.QuotaExceeded, r.watches.execute(Command("resume", Operation.Resume, suspended.id, suspended.version)))
        assertEquals(suspended, r.watches.observeList().first().single())
        r.fault("normal"); r.time += 1000
        assertTrue(r.watches.execute(Command("resume", Operation.Resume, suspended.id, suspended.version)) is Outcome.Success)
        val resumed = r.watches.observeList().first().single()
        assertEquals(WatchStatus.Active, resumed.status); assertEquals(r.time, resumed.baselineAt)
        assertTrue(r.findings.observeFindings().first().isEmpty())
    }
    @Test fun C13_unknownPublicationRemainsUnknownAndOnceCompletes() = runTest {
        val r = createRig(); r.activate(r.proposal(mode = FollowMode.Once)); r.check(); r.check()
        val result = r.findings.observeFindings().first().single()
        assertNull(result.publishedAt); assertEquals(r.time, result.observedAt)
        assertNotNull(result.eventAt)
        assertEquals(WatchStatus.Completed, r.watches.observeList().first().single().status)
    }
}

class DemoWatchContractTest : WatchContractTest() {
    override fun createRig(capacity: Int): Rig = object : Rig {
        override var time = 1_800_000_000_000L
        private var sequence = 0
        private val service = DemoService(Clock { time }, IdGenerator { "id-${++sequence}" }, capacity)
        override val watches: WatchRepository = service
        override val proposals: ProposalRepository = service
        override val findings: FindingRepository = service
        override val sessions: SessionRepository = service
        override suspend fun check() = service.simulateCheck()
        override suspend fun fault(name: String) = service.selectScenario(when (name) {
            "lost" -> DemoScenario.LostResponse
            "commandFailure" -> DemoScenario.CommandFailure
            "sourceFailure" -> DemoScenario.SourceFailure
            "empty" -> DemoScenario.NoResults
            "quota" -> DemoScenario.Quota
            else -> DemoScenario.Normal
        })
    }
}
