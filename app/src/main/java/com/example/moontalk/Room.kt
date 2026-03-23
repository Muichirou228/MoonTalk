package com.example.moontalk

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: String?,
    val user1_id: String?,
    val user2_id: String?,
    val created_at: String?
)