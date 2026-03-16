package com.example.moontalk

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val username: String,
    val learning_language: String,
    val is_online: Boolean = false,
    val is_searching: Boolean = false,
    val created_at: String? = null
)