package com.example.moontalk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.DisableContentCapture
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moontalk.ui.theme.MoonTalkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FindCompanions(initialProfile: Profile?, onSearchingChanged: (searching: Boolean) -> Unit, searchStatus: Boolean, context: Context){
    val rep = SupabaseRepository()
    var room by remember { mutableStateOf<Room?>(null) }
    var myProfile by remember { mutableStateOf<Profile?>(initialProfile) }
    var friendProfile by remember { mutableStateOf<Profile?>(null) }
    val task = rememberCoroutineScope()
    var seconds by remember { mutableStateOf(0) }
    var isSearching by remember { mutableStateOf(searchStatus) }
    var showAlert by remember {mutableStateOf(false)}
    var alertMessage by remember {mutableStateOf("")}
    var showChat by remember {mutableStateOf(false)}
    var isActive by remember {mutableStateOf(true)}
    suspend fun currentlySearchingForCompanion(){
        while (isSearching){
            if (!isActive) break
            var result = rep.startSearchCompanions(myProfile)
            if (result.isSuccess) {
                if (result.getOrNull() != null) {
                    friendProfile = result.getOrNull()
                    var result = rep.createRoom(myProfile, friendProfile)
                    if (result.isSuccess) {
                        room = result.getOrNull()
                    }
                    room = rep.deleteDuplicateRoom(myProfile, friendProfile).getOrNull()
                    isSearching = false
                    rep.changeUserSearching(false)
                    showChat = true
                    break
                }
            } else {
                alertMessage = result.exceptionOrNull().toString()
                showAlert = true
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            isActive = false
        }
    }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            seconds = 0
            while (isSearching) {
                delay(1000)
                seconds++
            }
        }
    }
    if (showChat) {
        AppState.currentRoomId = room?.id
        chat(myProfile, friendProfile, {showChat = false
                                       onSearchingChanged(false)
            AppState.currentRoomId = null}, room, {
                showChat = false
                isSearching = true
                onSearchingChanged(true)
            CoroutineScope(Dispatchers.IO).launch {
                rep.changeUserSearching(true)
                currentlySearchingForCompanion()
            }
        },
            context = context)
    } else {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Button (onClick = {
                task.launch {
                    if (isSearching) {
                        isSearching = false;
                        rep.changeUserSearching(isSearching)
                        onSearchingChanged(false)
                    } else {
                        isSearching = true;
                        rep.changeUserSearching(isSearching)
                        onSearchingChanged(true)
                        currentlySearchingForCompanion()
                    }
                }
            },
                modifier = Modifier
                    .size(160.dp)
                    .aspectRatio(1f),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isSearching) Color(0xFF9C27B0) else Color.LightGray,
                    contentColor = Color.White
                )) {
                Text(text = if (!isSearching) "Поиск" else "Отмена", textAlign = TextAlign.Center,
                    fontSize = 30.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier.height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSearching) {
                    Text(
                        text = formatTime(seconds),
                        color = Color.White,
                        fontSize = 24.sp
                    )
                }
            }
            if (showAlert) {
                MaterialAlert(alertMessage, {showAlert = false})
            }
        }
    }
}
fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

