package it.polimi.webeep.feature.sync.domain

import it.polimi.webeep.feature.dashboard.domain.model.Course

/** Ongoing and favourite courses first, so they are covered even if the run is cut short. */
object CourseSyncOrder {

    fun currentFirst(nowSeconds: Long): Comparator<Course> = compareByDescending<Course> { it.endDate == 0L || it.endDate > nowSeconds }
        .thenByDescending { it.isFavourite }
}
