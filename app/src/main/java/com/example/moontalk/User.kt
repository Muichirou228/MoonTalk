package com.example.moontalk

import kotlinx.serialization.Serializable

@Serializable
data class User (
    val id: Int,
    val login: String,
    val password: String,
    val nickname: String,
    val created_at: String,
    val learning_language: String,
    val is_online: Boolean,
    val is_searching: Boolean
)