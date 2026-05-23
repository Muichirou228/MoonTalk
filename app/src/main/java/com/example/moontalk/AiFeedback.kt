package com.example.moontalk

import kotlinx.serialization.Serializable

@Serializable
data class AiFeedback (
    val id: String? = null,
    val user_id: String? = null,
    val message_text: String? = null,
    val feedback_text: String? = null,
    val created_at: String? = null
)