package com.example.moontalk

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: String?,
    val userId1: String?,
    val userId2: String?,
    val created_at: String?
)