package fr.previensmoi.feature.create.presentation

import androidx.lifecycle.SavedStateHandle
import fr.previensmoi.data.demo.*
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateViewModelTest {
    private class Drafts : DraftRepository {
        var value = RequestDraft()
        var fail = false
        override suspend fun load(): Outcome<RequestDraft> = if (fail) Outcome.Error(Failure.StorageFailure) else Outcome.Success(value)
        override suspend fun save(draft: RequestDraft): Outcome<Unit> { if (fail) return Outcome.Error(Failure.StorageFailure); value = draft; return Outcome.Success(Unit) }
        override suspend fun clear(): Outcome<Unit> { value = RequestDraft(); return Outcome.Success(Unit) }
    }
    private class Fixture {
        var time = 1_000L
        var sequence = 0
        val ids = IdGenerator { "id-${++sequence}" }
        val service = DemoService(Clock { time }, ids)
        val drafts = Drafts()
        fun vm(proposals: ProposalRepository = service, saved: SavedStateHandle = SavedStateHandle()) =
            CreateViewModel(proposals, service, service, drafts, ids, saved)
    }
    private fun test(block: suspend TestScope.(Fixture) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block(Fixture()) } finally { Dispatchers.resetMain() }
    }

    @Test fun `duplicate text callback while evaluating does not cancel the current revision`() = test { f ->
        val deferred = CompletableDeferred<Outcome<Evaluation>>()
        val vm = f.vm(object : ProposalRepository by f.service {
            override suspend fun evaluate(draft: RequestDraft) = deferred.await()
        })
        runCurrent(); vm.updateText("A"); vm.evaluate("fr"); runCurrent()
        val revision = vm.state.value.draft.revision
        vm.updateText("A"); runCurrent()
        assertEquals(revision, vm.state.value.draft.revision)
        deferred.complete(Outcome.Success(Evaluation.Ready(Proposal("p",1,vm.state.value.draft,Long.MAX_VALUE))))
        runCurrent(); assertEquals(CreationStep.Ready, vm.state.value.step)
    }

    @Test fun `C09 late response for A cannot replace B even when cancellation is ignored`() = test { f ->
        val delayed = CompletableDeferred<Outcome<Evaluation>>()
        val proposals = object : ProposalRepository by f.service {
            override suspend fun evaluate(draft: RequestDraft): Outcome<Evaluation> =
                if (draft.text == "A") withContext(NonCancellable) { delayed.await() } else f.service.evaluate(draft)
        }
        val vm = f.vm(proposals); runCurrent()
        vm.updateText("A"); vm.evaluate("fr"); runCurrent()
        vm.updateText("B"); vm.evaluate("fr"); runCurrent()
        val b = vm.state.value.proposal
        delayed.complete(Outcome.Success(Evaluation.Ready(Proposal("old", 1, RequestDraft("A"), Long.MAX_VALUE))))
        runCurrent()
        assertEquals("B", b?.draft?.text); assertEquals(b, vm.state.value.proposal)
    }
    @Test fun `cancelled preview cannot resurrect after revise`() = test { f ->
        val delayed = CompletableDeferred<Outcome<Evaluation>>()
        val vm = f.vm(object : ProposalRepository by f.service {
            override suspend fun evaluate(draft: RequestDraft) = withContext(NonCancellable) { delayed.await() }
        }); runCurrent()
        vm.updateText("A"); vm.evaluate("fr"); runCurrent(); vm.revise()
        delayed.complete(Outcome.Success(Evaluation.Ready(Proposal("old", 1, RequestDraft("A"), Long.MAX_VALUE)))); runCurrent()
        assertEquals(CreationStep.Editing, vm.state.value.step); assertNull(vm.state.value.proposal)
    }
    @Test fun `C10 partial general request requires consent then login then another confirmation`() = test { f ->
        f.service.selectScenario(DemoScenario.Clarification)
        val vm = f.vm(); runCurrent(); vm.updateText("Nouveautes sur un sujet"); vm.updateMode(FollowMode.Continuous)
        vm.evaluate("fr"); runCurrent(); assertEquals(CreationStep.Clarifying, vm.state.value.step)
        vm.updateAnswer("Annonces et changements"); f.service.selectScenario(DemoScenario.Partial)
        vm.evaluate("fr"); runCurrent()
        vm.confirm(); runCurrent(); assertEquals(CreationStep.Ready, vm.state.value.step)
        assertTrue(f.service.observeList().first().isEmpty())
        vm.acceptPartial(true); vm.confirm(); assertEquals(CreationStep.Login, vm.state.value.step)
        vm.signIn(); runCurrent(); assertTrue(f.service.observeList().first().isEmpty())
        vm.confirm(); vm.confirm(); runCurrent()
        val watch = f.service.observeList().first().single()
        assertEquals(FollowMode.Continuous, watch.plan.draft.mode)
        assertEquals("Annonces et changements", watch.plan.draft.answer)
        assertEquals(watch.id, vm.state.value.activatedId)
        assertEquals(RequestDraft(), f.drafts.value)
    }
    @Test fun `lost activation reply retries same command after proposal expires`() = test { f ->
        f.service.signIn(); val vm = f.vm(); runCurrent(); vm.updateText("Exemple"); vm.evaluate("fr"); runCurrent()
        f.service.selectScenario(DemoScenario.LostResponse); vm.confirm(); runCurrent()
        assertEquals(Failure.TemporaryFailure, vm.state.value.error)
        val id = f.service.observeList().first().single().id
        f.time += 100_000_000L; vm.confirm(); runCurrent()
        assertEquals(id, vm.state.value.activatedId); assertEquals(1, f.service.observeList().first().size)
    }
    @Test fun `C14 restored proposal lost with process reports reset and restores draft without activation`() = test { f ->
        val saved = SavedStateHandle(); val first = f.vm(saved = saved); runCurrent()
        first.updateText("Brouillon conserve"); first.evaluate("fr"); runCurrent()
        assertNotNull(saved.get<String>("proposalId"))
        f.service.reset()
        val restored = f.vm(saved = saved); runCurrent()
        assertEquals("Brouillon conserve", restored.state.value.draft.text)
        assertTrue(restored.state.value.resetNotice)
        assertEquals(CreationStep.Editing, restored.state.value.step)
        assertTrue(f.service.observeList().first().isEmpty()); assertNull(restored.state.value.session)
    }
    @Test fun `draft storage failure is visible without pretending data is saved`() = test { f ->
        f.drafts.fail = true; val vm = f.vm(); runCurrent(); assertTrue(vm.state.value.storageFailed)
        vm.updateText("Local only"); runCurrent(); assertTrue(vm.state.value.storageFailed)
        f.drafts.fail = false; vm.updateText("Saved"); runCurrent()
        assertFalse(vm.state.value.storageFailed); assertEquals("Saved", f.drafts.value.text)
        vm.discard(); runCurrent(); assertEquals(RequestDraft(), f.drafts.value)
    }
    @Test fun `unsupported expired and quota never produce activation success`() = test { f ->
        val vm = f.vm(); runCurrent(); vm.updateText("Une demande")
        f.service.selectScenario(DemoScenario.Unsupported); vm.evaluate("fr"); runCurrent()
        assertEquals(CreationStep.Unsupported, vm.state.value.step)
        vm.revise(); f.service.signIn(); runCurrent()
        f.service.selectScenario(DemoScenario.Expired); vm.evaluate("fr"); runCurrent(); vm.confirm(); runCurrent()
        assertEquals(Failure.ProposalExpired, vm.state.value.error)
        vm.revise(); f.service.selectScenario(DemoScenario.Quota); vm.evaluate("fr"); runCurrent(); vm.confirm(); runCurrent()
        assertEquals(Failure.QuotaExceeded, vm.state.value.error)
        assertNull(vm.state.value.activatedId); assertTrue(f.service.observeList().first().isEmpty())
    }
    @Test fun `edit failure keeps confirmed plan and retry commits then clears draft`() = test { f ->
        f.service.signIn(); val vm = f.vm(); runCurrent(); vm.updateText("Before"); vm.evaluate("fr"); runCurrent(); vm.confirm(); runCurrent()
        val before = f.service.observeList().first().single()
        vm.consumeActivation(); vm.edit(before); vm.updateText("After"); vm.evaluate("fr"); runCurrent()
        f.service.selectScenario(DemoScenario.CommandFailure); vm.confirm(); runCurrent()
        assertEquals(before, f.service.observeList().first().single()); assertNull(vm.state.value.activatedId)
        f.service.selectScenario(DemoScenario.Normal); vm.confirm(); runCurrent()
        assertEquals("After", f.service.observeList().first().single().plan.draft.text)
        assertEquals(before.id, vm.state.value.activatedId)
    }
}
