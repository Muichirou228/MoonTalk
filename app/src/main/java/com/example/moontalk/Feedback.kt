package com.example.moontalk

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ktor.websocket.Frame

@Composable
fun Feedback() {
    Text("ТУТ БУДУТ ФИДБЕКИ", modifier = Modifier.fillMaxWidth().size(300.dp),
        color = Color.White)
}