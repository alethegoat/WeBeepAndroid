package it.polimi.webeep.core.common

/**
 * Thrown when an authenticated operation runs without a stored session token.
 */
class MissingTokenException : IllegalStateException("No session token")
