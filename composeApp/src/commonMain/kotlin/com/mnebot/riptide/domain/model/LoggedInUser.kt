package com.mnebot.riptide.domain.model

data class LoggedInUser(
    val id: String,
    val email: String,
    val displayName: String?,
    val avatarUrl: String?
)
