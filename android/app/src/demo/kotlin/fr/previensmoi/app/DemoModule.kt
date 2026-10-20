package fr.previensmoi.app

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.previensmoi.data.demo.DemoService
import fr.previensmoi.domain.model.Clock
import fr.previensmoi.domain.model.IdGenerator
import fr.previensmoi.domain.repository.*
import fr.previensmoi.data.local.LocalDraftRepository
import java.util.UUID
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DemoModule {
    @Provides fun clock(): Clock = Clock { System.currentTimeMillis() }
    @Provides fun ids(): IdGenerator = IdGenerator { UUID.randomUUID().toString() }
    @Provides @Singleton fun service(clock: Clock, ids: IdGenerator) = DemoService(clock, ids)
    @Provides fun watches(service: DemoService): WatchRepository = service
    @Provides fun proposals(service: DemoService): ProposalRepository = service
    @Provides fun findings(service: DemoService): FindingRepository = service
    @Provides fun session(service: DemoService): SessionRepository = service
    @Provides fun drafts(repository: LocalDraftRepository): DraftRepository = repository
}
