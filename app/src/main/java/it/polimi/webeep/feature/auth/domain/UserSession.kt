package it.polimi.webeep.feature.auth.domain

/**
 * Authenticated user profile independent of Moodle network DTOs.
 */
data class UserSession(val userId: Int, val fullName: String, val userName: String, val siteUrl: String, val avatarUrl: String)
