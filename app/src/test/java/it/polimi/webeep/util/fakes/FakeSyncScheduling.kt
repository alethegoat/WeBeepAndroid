package it.polimi.webeep.util.fakes

import it.polimi.webeep.feature.sync.domain.SyncRunStatus
import it.polimi.webeep.feature.sync.domain.SyncScheduling
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSyncScheduling(private val events: MutableList<String> = mutableListOf()) : SyncScheduling {
    var applyCount = 0
    val immediateRequests = mutableListOf<Boolean>()
    var cancelCount = 0

    override suspend fun applyCurrentSettings(replaceExisting: Boolean) {
        applyCount++
        events += "apply"
    }

    override suspend fun enqueueImmediate(manual: Boolean) {
        immediateRequests += manual
    }

    val oneShotStatus = MutableStateFlow(SyncRunStatus.IDLE)

    override fun oneShotStatus(): Flow<SyncRunStatus> = oneShotStatus

    override fun cancelAll() {
        cancelCount++
    }
}
