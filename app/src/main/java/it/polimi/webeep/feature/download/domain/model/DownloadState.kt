package it.polimi.webeep.feature.download.domain.model

/** Where a file stands on its way to local storage; shared by the course list and the file viewer. */
enum class DownloadState { NOT_STARTED, DOWNLOADING, COMPLETED, FAILED }
