package com.example.moontalk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ktor.websocket.Frame
import kotlinx.coroutines.launch

@Composable
fun chat(profile1: Profile?, profile2: Profile?, onCloseChat: () -> Unit, room: Room?) {
    val rep = SupabaseRepository()
    var scope = rememberCoroutineScope()
    var showAlert by remember { mutableStateOf(false) }
    var alertMessage by remember {mutableStateOf("")}
    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(profile1?.username?:"Not found", color = Color.White)
        Spacer(Modifier.width(10.dp))
        Text("IS TALKING TO", color = Color.White)
        Spacer(Modifier.width(10.dp))
        Text(profile2?.username?:"Not found" , color = Color.White)
    }
    Button({
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
        Text("BACK")
    }
    if (showAlert) {
        MaterialAlert(alertMessage, {showAlert = false})
    }
}