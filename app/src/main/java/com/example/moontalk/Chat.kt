package com.example.moontalk

import android.graphics.Paint
import android.util.Log
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.websocket.Frame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.TextStyle

@Composable
fun chat(profile1: Profile?, profile2: Profile?, onCloseChat: () -> Unit, room: Room?, onFindNewCompanion: () -> Unit) {
    val rep = SupabaseRepository()
    var scope = rememberCoroutineScope()
    var showAlert by remember { mutableStateOf(false) }
    var alertMessage by remember {mutableStateOf("")}
    var localRoom by remember {mutableStateOf<Room?>(room)}
    var messageText by remember { mutableStateOf("") }
    LaunchedEffect(room?.id) {
        if (room?.id != null) {
            while (true){
                delay(1000)
                var exists = rep.checkRoomExists(localRoom?.id.toString())
                if (!exists){
                    Log.d("ExSer", "Room doesnt exist")
                    alertMessage = "Собеседник закончил диалог"
                    showAlert = true
                    delay (1000)
                    onCloseChat()
                    break
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        alertMessage = "Room id is ${localRoom?.id}"
        showAlert = true
    }

    Column (Modifier.fillMaxSize().background(Color.Black)) {
        Row (Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column (horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    scope.launch {
                        if (room?.id != null) {
                            rep.deleteRoom(room.id)
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

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Сообщения будут здесь",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp
            )
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
                        // TODO: Отправить сообщение
                        Log.d("Chat", "Sending message: $messageText")
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9C27B0)),
                enabled = messageText.isNotBlank()
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Отправить", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }

    if (showAlert) {
        MaterialAlert(alertMessage, {showAlert = false})
    }
}