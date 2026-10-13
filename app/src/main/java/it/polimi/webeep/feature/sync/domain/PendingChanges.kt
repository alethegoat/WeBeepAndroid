package it.polimi.webeep.feature.sync.domain

import it.polimi.webeep.feature.sync.domain.model.CourseChanges

/** Pure bookkeeping of per-course change lists. */
object PendingChanges {

    /** Adds [fresh] to a list of per-course changes, merging with what the course already has. */
    fun append(existing: List<CourseChanges>, fresh: CourseChanges): List<CourseChanges> {
        if (fresh.changes.isEmpty()) return existing
        val old = existing.firstOrNull { it.courseId == fresh.courseId } ?: return existing + fresh
        val merged = fresh.copy(changes = FileChangeDetector.mergeChanges(old.changes, fresh.changes))
        return existing.map { if (it.courseId == fresh.courseId) merged else it }
    }

    /**
     * Keeps pending changes only for notifications still on screen, then adds [fresh] on top.
     */
    fun mergePending(pending: List<CourseChanges>, activeIds: Set<Int>, fresh: List<CourseChanges>): List<CourseChanges> =
        fresh.fold(pending.filter { it.courseId in activeIds }, ::append)
}
