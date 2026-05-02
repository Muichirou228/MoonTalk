package com.example.moontalk

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun MessageBubble(message: Message, isMyMessage: Boolean) {
    val audioPlayer = remember { AudioPlayerManager() }
    var isPlaying by remember { mutableStateOf(false) }
    var audioUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val rep = SupabaseRepository()

    LaunchedEffect(message.audio_message_id) {
        if (message.audio_message_id != null) {
            isLoading = true
            audioUrl = rep.getMessageAudioUrl(message)
            isLoading = false
        }
        isPlaying = false
        Log.d("Chat", "Message text is ${message.content}")
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMyMessage) Arrangement.Start else Arrangement.End  // ← End для своих
    ) {
        Box(
            Modifier.background(
                color = if (isMyMessage) Color(0xFF9C27B0) else Color(0xFF1E1E1E),
                shape = RoundedCornerShape(16.dp, 16.dp, if (isMyMessage) 4.dp else 16.dp,
                    if (isMyMessage) 16.dp else 4.dp)
            ).padding(horizontal = 16.dp, vertical = 12.dp).widthIn(max = 280.dp)
        ) {
            if (message.audio_message_id != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (audioUrl.isNullOrEmpty()) return@clickable

                        if (isPlaying) {
                            audioPlayer.stop()
                            isPlaying = false
                        } else {
                            audioPlayer.play(audioUrl!!) {
                                isPlaying = false
                            }
                            isPlaying = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Остановить" else "Воспроизвести",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isLoading -> "🔄 Загрузка..."
                            audioUrl == "e" || audioUrl == null -> "❌ Ошибка"
                            isPlaying -> "🎵 Играет"
                            else -> "🎤 Голосовое"
                        },
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            } else {
                Text(
                    message.content ?: "",
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = Color.White
                )
            }
        }
    }
}