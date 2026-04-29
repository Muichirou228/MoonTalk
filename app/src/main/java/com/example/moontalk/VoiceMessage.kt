package com.example.moontalk

import kotlinx.serialization.Serializable

@Serializable
data class VoiceMessage (
    val id: String? = null,
    val user_id: String?,
    val audio_url: String?,
    val transcript: String? = null,
    val created_at: String? = null,
)