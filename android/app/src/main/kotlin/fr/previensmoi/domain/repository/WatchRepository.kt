package fr.previensmoi.domain.repository

import fr.previensmoi.domain.model.RefreshResult
import fr.previensmoi.domain.model.Watch
import kotlinx.coroutines.flow.Flow

interface WatchRepository {
    fun observeList(): Flow<List<Watch>>
    suspend fun refresh(): RefreshResult
}
