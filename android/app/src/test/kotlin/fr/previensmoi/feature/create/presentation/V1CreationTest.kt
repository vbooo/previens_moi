package fr.previensmoi.feature.create.presentation

import androidx.lifecycle.SavedStateHandle
import fr.previensmoi.domain.model.*
import fr.previensmoi.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V1CreationTest {
    private class Fixture :
        ProposalRepository, WatchRepository, SessionRepository, DraftRepository {
        val session = MutableStateFlow<Session?>(null)
        val list = MutableStateFlow<List<Watch>>(emptyList())
        var draft = RequestDraft()
        var delayedLoad: CompletableDeferred<Outcome<RequestDraft>>? = null
        var delayedCommand: CompletableDeferred<Outcome<CommandReceipt>>? = null
        var proposal: Proposal? = null
        var evaluations = 0
        val commands = mutableListOf<Command>()
        var delayed: CompletableDeferred<Outcome<Proposal>>? = null

        override suspend fun evaluate(draft: RequestDraft): Outcome<Evaluation> {
            evaluations++
            proposal =
                Proposal(
                    "p",
                    1,
                    draft,
                    Long.MAX_VALUE,
                    partial = true,
                    backgroundPreparation = true,
                    method = "web_search",
                )
            return Outcome.Success(Evaluation.Ready(proposal!!))
        }

        override suspend fun get(id: String): Outcome<Proposal> = Outcome.Success(proposal!!)

        override suspend fun configure(proposal: Proposal, mode: FollowMode): Outcome<Proposal> =
            delayed?.let { withContext(NonCancellable) { it.await() } }
                ?: Outcome.Success(
                    proposal
                        .copy(
                            version = proposal.version + 1,
                            draft = proposal.draft.copy(mode = mode),
                        )
                        .also { this.proposal = it },
                )

        override fun observeList() = list

        override suspend fun refresh() = RefreshResult.Success

        override suspend fun execute(command: Command): Outcome<CommandReceipt> {
            commands += command
            delayedCommand?.let { return it.await() }
            return Outcome.Success(CommandReceipt(command.id, "w", 1))
        }

        override fun observeSession() = session

        override suspend fun signIn(): Outcome<Session> =
            Outcome.Success(Session("owner").also { session.value = it })

        override suspend fun load(): Outcome<RequestDraft> = delayedLoad?.await() ?: Outcome.Success(draft)

        override suspend fun save(draft: RequestDraft): Outcome<Unit> {
            this.draft = draft
            return Outcome.Success(Unit)
        }

        override suspend fun clear(): Outcome<Unit> {
            draft = RequestDraft()
            return Outcome.Success(Unit)
        }

        fun vm() =
            CreateViewModel(this, this, this, this, IdGenerator { "command" }, SavedStateHandle())
    }

    private fun test(block: suspend TestScope.(Fixture) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block(Fixture())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `country and website persist with request and no source work happens before confirmation`() =
        test { f ->
            val vm = f.vm()
            runCurrent()
            vm.startNew()
            vm.updateSite("https://shop.example/ps5")
            vm.updateText("Promotions PS5")
            runCurrent()
            assertEquals("France", f.draft.region)
            assertEquals("https://shop.example/ps5", f.draft.preferredSite)
            assertEquals(0, f.evaluations)
            vm.evaluate("fr")
            runCurrent()
            assertEquals(1, f.evaluations)
            assertTrue(f.commands.isEmpty())
            assertEquals(CreationStep.Ready, vm.state.value.step)
        }

    @Test
    fun `mode changes update confirmed preview without a second interpretation`() = test { f ->
        val vm = f.vm()
        runCurrent()
        vm.updateText("Promotions PS5")
        vm.evaluate("fr")
        runCurrent()
        vm.chooseMode(FollowMode.Continuous)
        runCurrent()
        assertEquals(1, f.evaluations)
        assertEquals(FollowMode.Continuous, vm.state.value.proposal?.draft?.mode)
        assertEquals(2L, vm.state.value.proposal?.version)
        f.signIn()
        runCurrent()
        vm.confirm()
        runCurrent()
        assertEquals(2L, f.commands.single().expectedVersion)
    }

    @Test
    fun `a late option update cannot resurrect a discarded confirmation`() = test { f ->
        val vm = f.vm()
        runCurrent()
        vm.updateText("A")
        vm.evaluate("fr")
        runCurrent()
        val old = f.proposal!!
        f.delayed = CompletableDeferred()
        vm.chooseMode(FollowMode.Continuous)
        runCurrent()
        vm.revise()
        vm.updateText("B")
        runCurrent()
        f.delayed!!.complete(
            Outcome.Success(old.copy(draft = old.draft.copy(mode = FollowMode.Continuous))),
        )
        runCurrent()
        assertNull(vm.state.value.proposal)
        assertEquals("B", vm.state.value.draft.text)
        assertFalse(vm.state.value.optionsUpdating)
    }

    @Test
    fun `sign in resumes the previously confirmed activation exactly once`() = test { f ->
        val vm = f.vm()
        runCurrent()
        vm.updateText("Concert Mira")
        vm.evaluate("fr")
        runCurrent()
        vm.confirm()
        assertEquals(CreationStep.Login, vm.state.value.step)
        f.signIn()
        runCurrent()
        vm.resumeAfterSignIn()
        vm.resumeAfterSignIn()
        runCurrent()
        assertEquals(1, f.commands.size)
        assertEquals("w", vm.state.value.activatedId)
    }

    @Test
    fun `invalid optional website stays on the request and never starts interpretation`() =
        test { f ->
            val vm = f.vm()
            runCurrent()
            vm.updateText("Concert Mira")
            vm.updateSite("http://localhost/admin")
            vm.evaluate("fr")
            runCurrent()
            assertTrue(vm.state.value.invalidSite)
            assertEquals(0, f.evaluations)
        }
    @Test fun `new resets restored text site clarification and edit context`() = test { f ->
        f.draft = RequestDraft("Old webhook request", answers = mapOf("preferred_site" to "https://old.example", "question" to "old"))
        val vm = f.vm(); runCurrent()
        vm.evaluate("fr"); runCurrent()
        vm.startNew(); runCurrent()
        assertEquals("", vm.state.value.draft.text)
        assertEquals(mapOf("region" to "France"), vm.state.value.draft.answers)
        assertEquals(CreationStep.Editing, vm.state.value.step)
        assertNull(vm.state.value.proposal)
        val watch = Watch("existing", Proposal("plan", 1, RequestDraft("Existing watch"), Long.MAX_VALUE), baselineAt = 0, version = 4)
        vm.edit(watch); runCurrent()
        assertEquals("Existing watch", vm.state.value.draft.text)
        assertEquals("existing", vm.state.value.editingId)
        vm.startNew(); runCurrent()
        assertEquals("", vm.state.value.draft.text)
        assertNull(vm.state.value.editingId)
    }

    @Test fun `late draft restoration cannot overwrite a new request`() = test { f ->
        f.delayedLoad = CompletableDeferred()
        val vm = f.vm(); runCurrent()
        vm.startNew(); runCurrent()
        f.delayedLoad!!.complete(Outcome.Success(RequestDraft("Old restored text"))); runCurrent()
        assertTrue(vm.state.value.loaded)
        assertEquals("", vm.state.value.draft.text)
    }

    @Test fun `a command response after sign out cannot navigate or activate in the new session`() = test { f ->
        f.signIn()
        val vm = f.vm(); runCurrent()
        vm.updateText("Concert Mira"); vm.evaluate("fr"); runCurrent()
        f.delayedCommand = CompletableDeferred()
        vm.confirm(); runCurrent()
        assertEquals(1, f.commands.size)
        f.session.value = null; runCurrent()
        f.delayedCommand!!.complete(Outcome.Success(CommandReceipt("command", "old-owner-watch", 1))); runCurrent()
        assertNull(vm.state.value.activatedId)
        assertNull(vm.state.value.proposal)
        assertEquals(CreationStep.Editing, vm.state.value.step)
    }

    @Test fun `restored background proposal remains confirmable after process recreation`() = test { f ->
        f.draft = RequestDraft("Concert Mira")
        f.proposal = Proposal("saved", 1, f.draft, Long.MAX_VALUE, partial = true, backgroundPreparation = true)
        val vm = CreateViewModel(f, f, f, f, IdGenerator { "command" }, SavedStateHandle(mapOf("proposalId" to "saved", "editId" to "existing", "editVersion" to 4L)))
        runCurrent()
        assertEquals(CreationStep.Ready, vm.state.value.step)
        assertTrue(vm.state.value.partialAccepted)
        assertEquals("existing", vm.state.value.editingId)
        f.signIn(); runCurrent(); vm.confirm(); runCurrent()
        assertEquals(Operation.Replace, f.commands.single().operation)
        assertEquals(4L, f.commands.single().expectedVersion)
    }

}
