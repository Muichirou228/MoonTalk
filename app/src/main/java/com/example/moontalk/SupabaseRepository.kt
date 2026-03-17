package com.example.moontalk

import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlin.collections.mapOf


class SupabaseRepository {
    private val client = SupabaseClient.client
    suspend fun signIn(email: String, password: String): Result<Profile> = try {
        val result = client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw Exception("Failed to get user after login")
        client.from("profiles").update(mapOf("is_online" to true)) {
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
            client.from("profiles").update(mapOf("is_searching" to false)) {
                filter { eq("id", userId) }
            }
        }
        Result.success(Unit);
    } catch (e: Exception) {
        Result.failure(e);
    }

    suspend fun register(
        userName: String,
        email: String,
        password: String,
        language: String
    ): Result<Profile> {
        return try {
            if (password.length < 6) return Result.failure(Exception("Пароль должен быть минимум 6 символов"))
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return Result.failure(Exception("Форма почты неверная"))
            val result = client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Couldn't find userid"))
            var profile = Profile(
                id = userId,
                username = userName,
                learning_language = language,
                is_online = true,
            )
            client.from("profiles").insert(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changeUserSearching(flag: Boolean): Result<Unit> {
        val userId = client.auth.currentUserOrNull()?.id.toString()
        client.from("profiles").update(mapOf("is_searching" to flag)) {
            filter { eq("id", userId) }
        }
        return Result.success(Unit)
    }

    suspend fun startSearchCompanions() {

    }
}

