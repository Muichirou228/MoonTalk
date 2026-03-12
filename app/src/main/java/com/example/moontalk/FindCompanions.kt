package com.example.moontalk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moontalk.ui.theme.MoonTalkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FindCompanions(){
    val rep = SupabaseRepository()
    val task = rememberCoroutineScope()

    var seconds by remember { mutableStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            while (isSearching) {
                delay(1000)
                seconds++
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Button (onClick = {
            task.launch {
                isSearching = true;
                rep.startSearchCompanions()
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
        if (isSearching) {
            Text("${seconds}", color = Color.White, fontSize = 24.sp)
        }
    }

}