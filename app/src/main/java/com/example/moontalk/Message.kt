package com.example.moontalk

import kotlinx.serialization.Serializable

@Serializable
data class Message (
    val id: String?,
    val room_id: String?,
    val user_id: String?,
    val content: String?,
    val created_at: String?
)