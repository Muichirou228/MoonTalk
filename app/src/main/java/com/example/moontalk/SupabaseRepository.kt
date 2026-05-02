package com.example.moontalk

import android.service.autofill.Validators.and
import android.service.autofill.Validators.or
import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.delay
import java.io.File
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

    suspend fun startSearchCompanions(profile: Profile?) : Result<Profile?> {
        return try {
            var language = getUserLanguage(profile)?.learning_language
            Log.d("FindCompanions", "Looking for companion with ${language}")
            if (profile?.id != null) {
                var result = client.from("profiles").select(){
                    filter { neq("id", profile?.id?:"")
                        eq("is_online", true)
                        eq ("is_searching", true)
                        eq("learning_language", language?:"")
                    }
                }.decodeSingleOrNull<Profile>()
                Result.success(result)
            } else {
                return Result.failure(Exception("Userid not found"))
            }

        } catch(e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRoom(profile1: Profile?, profile2: Profile?): Result<Room?> {
        return try {
            if (profile1 == null || profile2 == null) {
                return Result.failure(Exception("Profiles cannot be null"))
            }
            var roomAlreadyExist = findExistingRooms(profile1, profile2).getOrNull()
            if (roomAlreadyExist == null) {
                client.from("rooms")
                    .insert(
                        mapOf(
                            "user1_id" to profile1.id,
                            "user2_id" to profile2.id
                        )
                    )
            }
            var result = client.from("rooms").select { filter { eq("user1_id", profile1.id)
                eq("user2_id", profile2.id)} }.decodeSingleOrNull<Room>()
            if (result != null) {
                Result.success(result)
            } else {
                result = client.from("rooms").select { filter { eq("user2_id", profile1.id)
                    eq("user1_id", profile2.id)} }.decodeSingleOrNull<Room>()
                Result.success(result)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDuplicateRoom(profile1: Profile?, profile2: Profile?) : Result<Room?>{
        try {
            val firstId = if (profile1?.id.toString() < profile2?.id.toString()) profile1?.id else profile2?.id
            val secondId = if (profile1?.id.toString() < profile2?.id.toString()) profile2?.id else profile1?.id

            val room1 = client.from("rooms")
                .select() {
                    filter {
                        eq("user1_id", firstId.toString())
                        eq("user2_id", secondId.toString())
                    }
                }
                .decodeSingleOrNull<Room>()

            val room2 = client.from("rooms")
                .select() {
                    filter {
                        eq("user1_id", secondId.toString())
                        eq("user2_id", firstId.toString())
                    }
                }
                .decodeSingleOrNull<Room>()

            if (room1 != null && room2 != null) {
                Log.d("ExSer", "BOTH ROOMS FOUND ${room1.id} and ${room2.id}")
                client.from("rooms").delete() {
                        filter { eq("id", room1.id.toString()) }
                    }
                Log.d("ExSer", "Deleted ${room1.id}, returning ${room2.id}")
                return Result.success(room2)
            }
            if (room1 != null) {
                Log.d("ExSer", "Found only one room ${room1.id}")
                return Result.success(room1)
            }
            if (room2 != null) {
                Log.d("ExSer", "Found only one room ${room2.id}")
                return Result.success(room2)
            }
            Log.d("ExSer", "No rooms found")
            return Result.failure(Exception("No rooms found"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun findExistingRooms(profile1: Profile?, profile2: Profile?): Result<Room?>{
        val existingRoom = client.from("rooms")
            .select() {
                filter {
                    eq("user1_id", profile1?.id.toString())
                    eq("user2_id", profile2?.id.toString())
                }
            }
            .decodeSingleOrNull<Room>()

        if (existingRoom != null) {
            return Result.success(existingRoom)
        }
        val existingRoomReverse = client.from("rooms")
            .select() {
                filter { eq("user1_id", profile2?.id.toString())
                    eq("user2_id", profile1?.id.toString())}
            }
            .decodeSingleOrNull<Room>()

        if (existingRoomReverse != null) {
            return Result.success(existingRoomReverse)
        }
        return Result.success(null)
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

            response.audio_url
        } catch (e: Exception) {
            Log.e("AUDIOOO", "Error getting audio URL: ${e.message}")
            null
        }
    }
}



