package com.example.moontalk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessageBubble(message: Message, isMyMessage: Boolean) {
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
            Text(message.content?:"", fontSize = 16.sp,
                lineHeight = 22.sp, color = Color.White)
        }
    }
}