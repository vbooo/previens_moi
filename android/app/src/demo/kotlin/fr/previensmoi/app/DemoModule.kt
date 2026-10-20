package fr.previensmoi.app

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.previensmoi.data.demo.DemoWatchRepository
import fr.previensmoi.domain.repository.WatchRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class DemoModule {
    @Binds abstract fun watchRepository(implementation: DemoWatchRepository): WatchRepository
}
