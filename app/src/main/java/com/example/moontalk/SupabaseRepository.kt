package com.example.moontalk

import android.service.autofill.Validators.and
import android.service.autofill.Validators.or
import android.util.Log
import androidx.annotation.Nullable
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.delay
import java.io.File
import java.util.Objects.isNull
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
        changeUserOnline(true)
        val profile = client.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull<Profile>()
            ?: throw Exception("Profile not found")
        Result.success(profile)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun isUserLoggedIn() : Boolean {
        return client.auth.currentUserOrNull() != null
    }

    suspend fun getProfile(): Result<Profile?> {
        try {
            var userId = client.auth.currentUserOrNull()?.id
            if (userId != null) {
                return Result.success(client.from("profiles").select() {
                    filter { eq("id", userId) }
                }.decodeSingleOrNull<Profile>())
            } else {
                return Result.failure(Exception("No userid found"))
            }
        }catch (e: Exception) {
            return Result.failure(Exception(e.message))
        }
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

    suspend fun changeUserOnline(flag: Boolean): Result<Unit> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
            if (userId == null) {
                return Result.failure(Exception ("User is not auth"))
            }
            client.from("profiles").update(mapOf("is_online" to flag)) {
                filter { eq("id", userId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    }

    suspend fun getUserLanguage(profile: Profile?): Profile? {
        return client.from("profiles").select{
            filter { eq("id", profile?.id?:"") }
        }.decodeSingleOrNull<Profile>()
    }

    suspend fun test() {
        var result = client.from("profiles").select {
            filter{
                eq("learning_language", "Английский")
            }
        }.decodeList<Profile>()
        Log.d("FindCompanions", "found ${result.size} profiles")
    }

    suspend fun findWaitingRoom(userId: String, language: String): Result<Room?> {
        return try {
            Log.d("FindCompanions", "finding rooms")
            var waitroom = client.from("rooms").select{
                filter{
                    eq("status", "waiting")
                }
            }.decodeList<Room>()
            Log.d("FindCompanions", "Found ${waitroom.size} waiting rooms")
            if (!waitroom.isEmpty()) {
                for (room in waitroom) {
                    Log.d("FindCompanions", "Counting rooms, ${room.id}")
                    val creator = client.from("profiles")
                        .select()
                        {filter { eq("id", room.user1_id.toString()) }}
                        .decodeSingleOrNull<Profile>()

                    if (creator?.learning_language == language) {
                        Log.d("FindCompanions", "found room with language ${language}")
                        return Result.success(room)
                    }
                }
            }
            Log.d("FindCompanions", "returning null")
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun createWaitingRoom(userId: String): Result<Room?> {
        return try {
            Log.d("FindCompanions", "creating a room")
            client.from("rooms")
                .insert(mapOf(
                    "user1_id" to userId
                ))
            val room = client.from("rooms").select {
                filter{
                    eq ("user1_id", userId)
                }
            }.decodeSingleOrNull<Room>()
            Log.d("FindCompanions", "created a room")
            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinRoom(roomId: String?, userId: String): Result<Room?> {
        return try {
            Log.d("FindCompanions", "joining a room with id = ${roomId}")
            client.from("rooms")
                .update(
                    mapOf(
                        "user2_id" to userId,
                        "status" to "active"
                    )
                ) {
                    filter { eq("id", roomId!!) }
                }
            val room = client.from("rooms")
                .select()
                {filter { eq("id", roomId!!) }}
                .decodeSingleOrNull<Room>()

            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoomStatus(roomId: String?): Room? {
        return try {
            if (roomId == null) return null
            client.from("rooms")
                .select()
                {filter { eq("id", roomId) }}
                .decodeSingleOrNull<Room>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getFriendProfile(room: Room, myProfile: Profile): Profile? {
        try {
            var result1 = client.from("profiles").select {
                filter{
                    eq("id", room.user1_id.toString())
                }
            }.decodeSingleOrNull<Profile>()
            var result2 = client.from("profiles").select {
                filter{
                    eq("id", room.user2_id.toString())
                }
            }.decodeSingleOrNull<Profile>()
            if (myProfile.username == result1?.username) {
                return result2
            } else {
                return result1
            }
        } catch(e: Exception) {
            return null
        }
    }

    suspend fun deleteRoom(roomId: String): Result<Unit> {
        return try {
            client.from("rooms").delete { filter { eq("id", roomId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun checkRoomExists(roomId: String): Boolean {
        return try {
            //Log.d("ExSer", "id room is ${roomId}")
            val room = client.from("rooms")
                .select() {
                    filter {
                        eq("id", roomId)
                    }
                }
                .decodeSingleOrNull<Room>()
            room != null
        } catch (e: Exception) {
            Log.d("Chat", "Error is ${e.message}")
            false
        }
    }

    suspend fun updateUserLanguage(language: String): Result<Unit> {
        try {
            client.from("profiles").update(mapOf("learning_language" to language)){
                filter { eq("id", client.auth.currentUserOrNull()?.id.toString()) }
            }
            return Result.success(Unit)
        }catch (e:Exception) {
            return Result.failure(e)
        }
    }

    suspend fun sendMessage(message: Message): Result<Unit> {
        try {
            client.from("messages").insert(
                mapOf("room_id" to message.room_id,
                    "user_id" to message.user_id,
                    "content" to message.content,
                    )
            )
            Log.d("Chat", "INSERTED")
            return Result.success(Unit)
        } catch(e: Exception){
            Log.d("Chat", "Exception ${e.message}")
            return Result.failure(e)
        }
    }

    suspend fun listenForNewMessages(room_id: String): List<Message> {
        try {
            return client.from("messages").select {
                filter { eq("room_id", room_id)}
            }.decodeList<Message>()
        } catch(e: Exception) {
            Log.d("Chat", "Exception while listening ${e.message}")
            return emptyList()
        }
    }

    suspend fun deleteAllMessagesFromRoom(room_id:String) {
        try {
            client.from("messages").delete { filter { eq("room_id", room_id) } }
        }catch (e: Exception) {
            Log.d("Chat", "Exception while deleting ${e.message}")
        }
    }

    suspend fun sendVoiceMessageWithFile(roomId: String, userId: String?, audioFile: File?): Result<Unit> {
        return try {
            val fileName = "voice_${System.currentTimeMillis()}.m4a"
            val path = "voice_messages/$roomId/$fileName"
            if (audioFile != null) {
                client.storage.from("voice_bucket")
                    .upload(path, audioFile.readBytes())
            } else {
                throw Exception("AUDIO FILE IS NULL")
            }
            Log.d("AUDIOOO", "uploaded file in bucket")
            val audioUrl = client.storage.from("voice_bucket").publicUrl(path)
            Log.d("AUDIOOO", "took file from bucket, ${audioUrl}, inserting in table with data ${userId}, ${roomId}")
            var VM = VoiceMessage(user_id = userId, audio_url = audioUrl)
            client.from("voice_messages")
                .insert(
                    mapOf(
                        "user_id" to VM.user_id,
                        "audio_url" to VM.audio_url,
                    )
                )
            Log.d("AUDIOOO", "inserted in table")
            val voiceMessage = client.from("voice_messages")
                .select() {
                    filter { eq("audio_url", audioUrl) }
                }
                .decodeSingle<VoiceMessage>()
            Log.d("AUDIOOO", "took from table")
            Log.d("AUDIOOO", "voice id is ${voiceMessage.id}")
            var defaultMessage = Message(room_id = roomId, user_id = userId, content = null, audio_message_id = voiceMessage.id, id = null, created_at = null)
            client.from("messages").insert(
                mapOf(
                    "room_id" to defaultMessage.room_id,
                    "user_id" to defaultMessage.user_id,
                    "audio_message_id" to defaultMessage.audio_message_id,
                )
            )

            audioFile?.delete()

            Log.d("AUDIOOO", "Voice message sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AUDIOOO", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getMessageAudioUrl(message: Message): String? {
        return try {
            if (message.audio_message_id == null) return null

            val response = client.from("voice_messages")
                .select()
                {filter { eq("id", message.audio_message_id) }}
                .decodeSingle<VoiceMessage>()
            Log.d("Chat", "This message audio url is ${response.audio_url}")
            response.audio_url
        } catch (e: Exception) {
            Log.e("AUDIOOO", "Error getting audio URL: ${e.message}")
            null
        }
    }
}



