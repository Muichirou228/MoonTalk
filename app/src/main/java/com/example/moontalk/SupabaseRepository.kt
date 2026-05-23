package com.example.moontalk

import android.service.autofill.Validators.and
import android.service.autofill.Validators.or
import android.util.Log
import androidx.annotation.Nullable
import com.google.gson.Gson
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Objects.isNull
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.collections.mapOf


class SupabaseRepository {

    private data class GigaChatAuth(val access_token: String, val expires_at: Long)
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            // Создаем TrustManager, который доверяет всем сертификатам
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            val sslSocketFactory = sslContext.socketFactory

            val builder = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true } // Отключает проверку имени хоста
                .build()
            return builder
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
    suspend fun analyzeTextWithGigaChat(text: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val client = getUnsafeOkHttpClient()

                // 1. Получаем Access Token
                val authUrl = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
                val authBody = "scope=GIGACHAT_API_PERS".toRequestBody("application/x-www-form-urlencoded".toMediaType())

                val authRequest = Request.Builder()
                    .url(authUrl)
                    .post(authBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Accept", "application/json")
                    .addHeader("RqUID", UUID.randomUUID().toString())
                    .addHeader("Authorization", "Basic MDE5ZTRlZmEtOWQyOC03ZjZmLWI0M2QtMmY4MDQ1ZWM0YTk5OmM5ZTQwNDkxLTUwYmEtNDU3NS05MDhhLWYyNjczYTdhMDNkMg==")
                    .build()

                val authResponse = client.newCall(authRequest).execute()
                if (!authResponse.isSuccessful) {
                    return@withContext Result.failure(Exception("Auth failed: ${authResponse.code}"))
                }

                val authJson = Gson().fromJson(authResponse.body?.string(), JsonObject::class.java)
                val accessToken = authJson.get("access_token").asString

                // 2. Отправляем текст на анализ
                val chatUrl = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"

                val prompt = """
    Ты — профессиональный репетитор английского языка.
    Проанализируй этот текст и верни ответ в виде обычного текста, без JSON, но с четким разделением на три блока.

    Отвечай строго в таком формате:

    Сильные стороны:
    [напиши, что сделано хорошо]

    Слабые стороны/ошибки:
    [перечисли ошибки]

    Совет по улучшению:
    [дай рекомендации]

    Текст пользователя: "$text"
""".trimIndent()

                val jsonBody = JsonObject().apply {
                    addProperty("model", "GigaChat:latest")

                    val messagesArray = JsonArray()
                    val userMessage = JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", prompt)
                    }
                    messagesArray.add(userMessage)
                    add("messages", messagesArray)
                }

                val chatRequest = Request.Builder()
                    .url(chatUrl)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/json")
                    .build()

                val chatResponse = client.newCall(chatRequest).execute()
                if (!chatResponse.isSuccessful) {
                    return@withContext Result.failure(Exception("Analysis failed: ${chatResponse.code}"))
                }

                val responseText = chatResponse.body?.string() ?: ""

                // Парсим ответ, чтобы достать content
                val jsonResponse = JsonParser.parseString(responseText).asJsonObject
                val choices = jsonResponse.getAsJsonArray("choices")
                val message = choices[0].asJsonObject.get("message").asJsonObject
                val content = message.get("content").asString

                Result.success(content)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
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

    suspend fun getProfile(): Result<Profile> {
        try {
            var userId = client.auth.currentUserOrNull()?.id
            Log.d("Chat", "User id is ${userId}")
            if (userId != null) {
                Log.d("Chat", "User id is not null, getting profile")
                var result = client.from("profiles").select() {
                    filter { eq("id", userId) }
                }.decodeSingle<Profile>()
                Log.d("Chat", "got profile ${result?.id}")
                return Result.success(result)
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
            Log.d("FindCompanions", "joining a room with id = ${roomId} and ${userId}")
            val room = client.from("rooms")
                .update(
                    mapOf(
                        "user2_id" to userId,
                        "status" to "active"
                    )
                ) {
                    filter { eq("id", roomId!!) }
                    select()
                }.decodeSingleOrNull<Room>()
            Log.d("FindCompanions", "joining a room with id = ${roomId}, SUCCESS")
            Result.success(room)
        } catch (e: Exception) {
            Log.d("FindCompanions", "ERROR : ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getRoomStatus(roomId: String?): Room? {
        return try {
            Log.d("FindCompanions", "Trying to get roomstatus for ${roomId}")
            if (roomId == null) return null
            client.from("rooms")
                .select()
                {filter { eq("id", roomId) }}
                .decodeSingleOrNull<Room>()
        } catch (e: Exception) {
            Log.d("FindCompanions", "ERRPR : ${e.message}")
            null
        }
    }

    suspend fun getFriendProfile(room: Room, myProfile: Profile): Profile? {
        try {
            val friendId = if (room.user1_id == myProfile.id) room.user2_id else room.user1_id
            Log.d("FindCompanions", "friendId = $friendId")

            Log.d("FindCompanions", "Запрос к profiles...")
            val friend = client.from("profiles")
                .select()
                {filter { eq("id", friendId ?: return null) }}
                .decodeSingleOrNull<Profile>()
            Log.d("FindCompanions", "Запрос выполнен, результат = ${friend?.username}")

            return friend

        } catch(e: Exception) {
            Log.d("FindCompanions", "ERROR in getFriendProfile: ${e.message}")
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

    suspend fun saveAIFeedback (message: String, feedback: String): Result<Unit> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
            val example = AiFeedback(user_id = userId, message_text = message, feedback_text = feedback)
            client.from("ai_feedback").insert(
                mapOf(
                    "user_id" to example.user_id,
                    "message_text" to example.message_text,
                    "feedback_text" to example.feedback_text
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.d("Chat", "Error saving ai ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getAIFeedbacks(): List<AiFeedback> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id ?: return emptyList()

            client.from("ai_feedback")
                .select()
                {filter { eq("user_id", userId) }}
                .decodeList<AiFeedback>()
        } catch (e: Exception) {
            Log.e("FEEDBACK", "Error loading: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAccountCreatedYear(): String {
        return try {
            val userId = client.auth.currentUserOrNull()?.id ?: return "2026"

            val profile = client.from("profiles")
                .select()
                {filter { eq("id", userId) }}
                .decodeSingleOrNull<Profile>()

            val createdAtString = profile?.created_at ?: return "2026"

            // Парсим "2026-05-23T10:30:00.000Z" → "2026"
            val year = createdAtString.substring(0, 4)
            year
        } catch (e: Exception) {
            Log.e("PROFILE", "getAccountCreatedYear error: ${e.message}")
            "2026"
        }
    }

    suspend fun getFeedbacksCount(): Int {
        return try {
            val userId = client.auth.currentUserOrNull()?.id ?: return 0
            Log.d("Chat", "trying ti get feedbackscount")
            val result = client.from("ai_feedback")
                .select()
                {filter { eq("user_id", userId) }}
                .decodeList<AiFeedback>()
            Log.d("Chat", "Count: ${result.size}")
            result.size
        } catch (e: Exception) {
            Log.d("Chat", "ERROR: ${e.message}")
            0
        }
    }

    suspend fun getVoiceCount(): Int {
        return try {
            val userId = client.auth.currentUserOrNull()?.id ?: return 0
            Log.d("Chat", "trying ti get voicecount")
            val result = client.from("voice_messages")
                .select()
                {filter { eq("user_id", userId) }}
                .decodeList<VoiceMessage>()
            Log.d("Chat", "voice count is ${result.size}")
            result.size
        } catch (e: Exception) {
            Log.d("Chat", "ERROR: ${e.message}")
            0
        }
    }
}



