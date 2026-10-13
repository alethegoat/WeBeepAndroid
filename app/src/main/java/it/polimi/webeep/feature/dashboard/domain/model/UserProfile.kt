package it.polimi.webeep.feature.dashboard.domain.model

/** Signed-in user shown in the dashboard header; [avatarUrl] is token-free and `null` when there is no picture. */
data class UserProfile(val fullName: String, val avatarUrl: String?)
