package it.polimi.webeep.feature.sync.domain

/** State of the one-off sync, as the scheduler sees it. */
enum class SyncRunStatus {
    /** No one-off sync is waiting or running (never started, finished, failed or cancelled). */
    IDLE,

    /** Enqueued but not started yet, typically waiting for its network or battery constraints. */
    QUEUED,

    /** Running now. */
    RUNNING
}
