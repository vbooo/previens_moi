package fr.previensmoi.data.demo

import fr.previensmoi.domain.model.RefreshResult
import fr.previensmoi.domain.model.Watch
import fr.previensmoi.domain.repository.WatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoWatchRepository @Inject constructor() : WatchRepository {
    private val watches = MutableStateFlow<List<Watch>>(emptyList())
    override fun observeList() = watches.asStateFlow()
    override suspend fun refresh(): RefreshResult {
        watches.value = listOf(Watch("synthetic-concert-example"))
        return RefreshResult.Success
    }
}
