package it.polimi.webeep.util.fakes

import it.polimi.webeep.core.session.SessionDataCleaner
import kotlinx.coroutines.CompletableDeferred

class FakeSessionDataCleaner : SessionDataCleaner {
    var wipeCount = 0
    var maxConcurrent = 0
    var failure: Throwable? = null

    /** When set, [wipe] suspends until it completes, to hold the logout mutex. */
    var gate: CompletableDeferred<Unit>? = null
    private var running = 0

    override suspend fun wipe() {
        wipeCount++
        running++
        maxConcurrent = maxOf(maxConcurrent, running)
        try {
            gate?.await()
            failure?.let { throw it }
        } finally {
            running--
        }
    }
}
