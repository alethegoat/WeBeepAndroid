package it.polimi.webeep.feature.sync.domain

/** Keys shared by the sync notifications and the screen that handles their tap. */
object SyncExtras {
    /** Intent extra naming the course a notification opens. Already-posted notifications carry it, so the value must not change. */
    const val COURSE_ID = "EXTRA_COURSE_ID"
}
