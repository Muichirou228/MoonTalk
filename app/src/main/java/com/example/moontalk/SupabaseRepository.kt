package com.example.moontalk

import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlin.collections.mapOf


class SupabaseRepository {
    private val client = SupabaseClient.client
    suspend fun signIn (email: String, password: String): Result<Profile> = try {
        val result = client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw Exception("Failed to get user after login")
        client.from("profiles").update (mapOf("is_online" to true)){
            filter { eq("id", userId) }
        }
        val profile = client.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull<Profile>()
            ?: throw Exception("Profile not found")
        Result.success(profile)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun notOnlineAnymore(): Result<Unit> = try {
        val userId = client.auth.currentUserOrNull()?.id
        if (userId != null) {
            client.from("profiles").update(mapOf("is_online" to false)) {
                filter { eq("id", userId) }
            }
        }
        Result.success(Unit);
        } catch (e: Exception) {
            Result.failure(e);
        }

    suspend fun startSearchCompanions() {

    }
}

