package it.polimi.webeep.feature.sync.domain

import it.polimi.webeep.feature.dashboard.domain.model.Course

/** Ongoing and favourite courses first, so they are covered even if the run is cut short. */
object CourseSyncOrder {

    /**
     * Comparator that puts ongoing courses (no end date, or ending after [nowSeconds]) before finished ones,
     * favourites first within each group.
     */
    fun currentFirst(nowSeconds: Long): Comparator<Course> = compareByDescending<Course> { it.endDate == 0L || it.endDate > nowSeconds }
        .thenByDescending { it.isFavourite }
}
