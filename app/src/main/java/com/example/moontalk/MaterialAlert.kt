package com.example.moontalk

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MaterialAlert(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {Text("MoonTalk",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())},
        text = {Text(message,
            fontSize = 24.sp,
            modifier = Modifier.fillMaxWidth())},
        confirmButton = {
            TextButton(onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF9C27B0)
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("OK", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color(0xFF9C27B0),
        textContentColor = Color.White
    )
}