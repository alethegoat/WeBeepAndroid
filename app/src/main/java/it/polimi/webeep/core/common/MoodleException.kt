package it.polimi.webeep.core.common

import java.io.IOException

/** Error reported by a Moodle web service; an [IOException] so generic network handling still applies. */
class MoodleException(val errorCode: String, message: String?) : IOException(message)
