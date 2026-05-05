package com.example.moontalk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import io.github.jan.supabase.gotrue.auth
import io.ktor.websocket.Frame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.io.File
import java.time.format.TextStyle
import kotlin.collections.mutableListOf
import kotlin.uuid.Uuid


@Composable
fun chat(profile1: Profile?, profile2: Profile?, onCloseChat: () -> Unit, room: Room?, onFindNewCompanion: () -> Unit, context: Context) {
    val rep = SupabaseRepository()
    var scope = rememberCoroutineScope()
    var showAlert by remember { mutableStateOf(false) }
    var currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    val listState = rememberLazyListState()
    var alertMessage by remember {mutableStateOf("")}
    var localRoom by remember {mutableStateOf<Room?>(room)}
    var messageText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var makingAudioMessage by remember { mutableStateOf(false) }
    var audioRecorderManager by remember { mutableStateOf<AudioRecorderManager?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
        } else {
            alertMessage = "Нужно разрешение на запись аудио"
            showAlert = true
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(localRoom?.id) {
        if (localRoom?.id != null) {
            while (true) {
                delay(2000)
                var exists = rep.checkRoomExists(localRoom?.id.toString())
                if (!exists){
                    Log.d("ExSer", "Room doesnt exist")
                    alertMessage = "Собеседник закончил диалог"
                    showAlert = true
                    delay(2000)
                    onCloseChat()
                    break
                }
                val newMessages = rep.listenForNewMessages(localRoom?.id.toString())
                if (newMessages != messages) {
                    messages = newMessages
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        alertMessage = "Room id is ${localRoom?.id}"
        showAlert = true
    }

    LaunchedEffect(Unit) {
        audioRecorderManager = AudioRecorderManager(context)
1    }

    fun sendMessage(){
        if (messageText.isNotBlank() && SupabaseClient.client.auth.currentUserOrNull()?.id != null && localRoom?.id != null) {
            val newMessage = Message(
                id = null,
                room_id = localRoom?.id,
                user_id = profile1?.id,
                content = messageText,
                created_at = null,
                audio_message_id = null
            )
            scope.launch {
                Log.d("Chat", "SENDING")
                rep.sendMessage(newMessage)
            }
            messageText = ""
        }
    }
    fun sendVoiceMessage(audioFile: File?) {
        if (room?.id != null && profile1?.id != null) {
            scope.launch {
                makingAudioMessage = true

                val result = rep.sendVoiceMessageWithFile(
                    roomId = localRoom?.id!!,
                    userId = profile1?.id,
                    audioFile = audioFile
                )

                if (result.isFailure) {
                    alertMessage = result.exceptionOrNull()?.message ?: "Ошибка отправки"
                    showAlert = true
                }

                makingAudioMessage = false
            }
        }
    }
    Column (Modifier.fillMaxSize().background(Color.Black)) {
        Row (Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column (horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    scope.launch {
                        if (localRoom?.id != null) {
                            rep.deleteRoom(localRoom?.id!!)
                            rep.deleteAllMessagesFromRoom(localRoom?.id!!)
                            onCloseChat()
                        } else {
                            alertMessage = "ROOM ID IS NULL"
                            showAlert = true
                        }
                    }
                }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color(0xFF9C27B0), modifier = Modifier.size(28.dp))
                }
                Text(
                    text = "Закончить",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            //Spacer(modifier = Modifier.width(12.dp))
            Row (verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(60.dp).clip(CircleShape).background(Color(0xFF9C27B0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile2?.username?.take(1)?.uppercase() ?: "?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        //modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.width(15.dp))
                Text(
                    profile2?.username ?: "???",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            //Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        scope.launch {
                            if (room?.id != null) {
                                rep.deleteRoom(room.id)
                                rep.deleteAllMessagesFromRoom(room.id)
                                onCloseChat()
                                onFindNewCompanion()
                            } else {
                                alertMessage = "ROOM ID IS NULL"
                                showAlert = true
                            }
                        }

                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Новый собеседник",
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Следующий",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Divider(
            color = Color.Gray.copy(alpha = 0.3f),
            thickness = 1.dp
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = false,
            state = listState
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isMyMessage = message.user_id == currentUserId
                )
            }
        }

        Divider(
            color = Color.Gray.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
        Row (modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom){
            BasicTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 16.sp
                ),
                decorationBox = { innerTextField ->
                    if (messageText.isEmpty()) {
                        Text(
                            text = "Сообщение...",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 16.sp,
                            //modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        Log.d("Chat", "Sending message ${messageText}")
                        sendMessage()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9C27B0)),
                enabled = messageText.isNotBlank() && !isRecording
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Отправить", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer (Modifier.width(5.dp))
            IconButton(
                onClick = {
                    if (isRecording) {
                        val audioFile = audioRecorderManager?.stopRecording()
                        isRecording = false
                        Log.d("AUDIOOO", audioFile?.absolutePath.toString())
                        sendVoiceMessage(audioFile)
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@IconButton
                            }
                        }
                        val success = audioRecorderManager?.startRecording() == true
                        if (success) {
                            isRecording = true
                        } else {
                            alertMessage = "Не удалось начать запись"
                            showAlert = true
                        }
                    }
                },
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isRecording) Color.Red else Color(0xFF9C27B0))
            ) {
                Icon (imageVector = if (isRecording) Icons.Default.Close else Icons.Default.Call, contentDescription = "GS", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }

    if (showAlert) {
        MaterialAlert(alertMessage, {showAlert = false})
    }
}