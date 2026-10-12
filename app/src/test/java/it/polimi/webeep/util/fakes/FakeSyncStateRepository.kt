package it.polimi.webeep.util.fakes

import it.polimi.webeep.feature.sync.domain.SyncStateRepository
import it.polimi.webeep.feature.sync.domain.model.SyncState
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSyncStateRepository : SyncStateRepository {
    override val lastCheckAt = MutableStateFlow<Long?>(null)
    var state = SyncState()

    override suspend fun <T> update(block: (SyncState) -> Pair<SyncState, T>): T {
        val (next, result) = block(state)
        state = next
        return result
    }

    override suspend fun clear() {
        state = SyncState()
    }
}
