package com.example.moontalk

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Account(profile: Profile?) {
    val rep = SupabaseRepository()
    var profile by remember { mutableStateOf<Profile?>(profile) }
    var errorMessage by remember {mutableStateOf<String?>(null)}
    Text(text = errorMessage ?: (profile?.username ?: "Не найдено"), modifier = Modifier.size(400.dp),
        color = Color.White)
}