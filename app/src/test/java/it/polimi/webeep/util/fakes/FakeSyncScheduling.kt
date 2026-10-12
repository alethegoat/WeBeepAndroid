package it.polimi.webeep.util.fakes

import it.polimi.webeep.feature.sync.domain.SyncScheduling

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

    override fun cancelAll() {
        cancelCount++
    }
}
