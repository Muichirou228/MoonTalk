package com.example.moontalk

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val username: String,
    val learning_language: String,
    val is_online: Boolean,
    val is_searching: Boolean,
    val created_at: String
)