package it.polimi.webeep.feature.settings.domain.model

/** Persisted by [name]: renaming a constant would reset users' saved frequency. */
enum class SyncFrequency {
    FIFTEEN_MINUTES,
    ONE_HOUR,
    TWELVE_HOURS,
    DAILY,
    NEVER;

    companion object {
        val DEFAULT = ONE_HOUR
    }
}
