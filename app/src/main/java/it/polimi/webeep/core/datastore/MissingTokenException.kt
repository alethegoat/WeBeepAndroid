package it.polimi.webeep.core.datastore

/**
 * Thrown when an authenticated operation runs without a stored session token.
 */
class MissingTokenException : IllegalStateException("No session token")
