package com.example.moontalk

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessageBubble(message: Message, isMyMessage: Boolean) {
    var audioPlayer by remember { mutableStateOf<AudioPlayerManager?>(AudioPlayerManager()) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(message.audio_message_id) {
        // Сброс состояния при новом сообщении
        isPlaying = false
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMyMessage) Arrangement.Start else Arrangement.End
    ) {
        Box(Modifier.background(
            color = if (isMyMessage) Color(0xFF9C27B0) else Color(0xFF1E1E1E),
            shape = RoundedCornerShape (16.dp, 16.dp, if (isMyMessage) 4.dp else 16.dp,
                if (isMyMessage) 16.dp else 4.dp)
        ).padding(horizontal = 16.dp, vertical = 12.dp).widthIn(max = 280.dp)
        ) {
            if (message.audio_message_id != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable{
                        //взять с бакета ссылку на аудио
                        val audioUrl = "https://nfzhklakrgnjleuacrzw.supabase.co/storage/v1/object/public/voice_bucket/voice_messages/5d1708df-33d9-4917-9f01-358a350500bd/voice_1777476525981.m4a"
                        if (isPlaying) {
                            audioPlayer?.stop()
                            isPlaying = false
                        } else {
                            audioPlayer?.play(audioUrl) {
                                isPlaying = false
                            }
                            isPlaying = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlaying) "00:00" else "🎤 Голосовое",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            } else {
                Text(message.content?:"", fontSize = 16.sp,
                    lineHeight = 22.sp, color = Color.White)
            }

        }
    }
}