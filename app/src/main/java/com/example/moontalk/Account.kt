package com.example.moontalk

import android.util.Log
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CheckboxDefaults.colors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
@Composable
fun Account(profile: Profile?, onLogout: () -> Unit) {
    val rep = SupabaseRepository()
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var days by remember {mutableStateOf("")}
    var isLoading by remember {mutableStateOf(true)}
    var voice by remember {mutableStateOf(0)}
    var feedbacks by remember {mutableStateOf(0)}
    var isDropMenuDropped by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Загрузка...") }
    var isUpdatingLanguage by remember { mutableStateOf(false) }
    var showMaterialAlert by remember { mutableStateOf(false) }
    val languages = listOf(
        "Английский", "Испанский", "Французский", "Немецкий",
        "Итальянский", "Португальский", "Китайский", "Японский",
        "Корейский"
    )
    LaunchedEffect(Unit) {
        try {
            selectedLanguage = rep.getUserLanguage(profile)?.learning_language?:""
            days = rep.getAccountCreatedYear()
            feedbacks = rep.getFeedbacksCount()
            voice = rep.getVoiceCount()
        } catch (e: Exception) {
            Log.e("Chat", "ERROR: ${e.message}")
            selectedLanguage = "Английский"
            days = ""
            feedbacks = 0
            voice = 0
        } finally {
            isLoading = false
        }

    }
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF9C27B0))
        }
        return
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp).background(Color.Black)) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.Black),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF9C27B0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile?.username?.take(1)?.uppercase() ?: "?",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(15.dp))
                    Text(
                        profile?.username ?: "???",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = {
                    showLogoutDialog = true
                }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp, modifier = Modifier.size(30.dp),
                        contentDescription = "Выйти", tint = Color(0xFF9C27B0)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Изучаемый язык",
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(color = Color.White, width = 3.dp),
                            onValueChange = {},
                            value = selectedLanguage,
                            readOnly = true,
                            textStyle = TextStyle(color = Color.White),
                            placeholder = {
                                Text(
                                    text = selectedLanguage,
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    fontSize = 20.sp
                                )
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { isDropMenuDropped = true }
                        )
                        DropdownMenu(
                            expanded = isDropMenuDropped,
                            onDismissRequest = { isDropMenuDropped = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E))
                        ) {
                            languages.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(text = language, color = Color.White) },
                                    onClick = {
                                        selectedLanguage = language
                                        isDropMenuDropped = false
                                        isUpdatingLanguage = true
                                        scope.launch {
                                            var result = rep.updateUserLanguage(language)
                                            isUpdatingLanguage = false
                                            if (result.isSuccess) {
                                                showMaterialAlert = true
                                            }
                                        }

                                    }
                                )
                            }
                        }
                    }
                    if (isUpdatingLanguage) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF9C27B0),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Обновление...",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = voice.toString(),
                            fontSize = 20.sp,
                            color = Color(0xFF9C27B0),
                            fontWeight = FontWeight.Bold
                        )
                        Text("голосовых", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = feedbacks.toString(),
                            fontSize = 20.sp,
                            color = Color(0xFF9C27B0),
                            fontWeight = FontWeight.Bold
                        )
                        Text("фидбеков", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = days.toString(),
                            fontSize = 20.sp,
                            color = Color(0xFF9C27B0),
                            fontWeight = FontWeight.Bold
                        )
                        Text("год", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
        if (showMaterialAlert) {
            MaterialAlert("Язык успешно изменен", {showMaterialAlert = false})
        }
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Выход", color = Color.White) },
                text = { Text("Вы уверены, что хотите выйти?", color = Color.White) },
                containerColor = Color(0xFF1E1E1E),
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                rep.notOnlineAnymore()
                                onLogout()
                            }
                            showLogoutDialog = false
                        }
                    ) {
                        Text("Выйти", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Отмена", color = Color(0xFF9C27B0))
                    }
                })
        }
    }

    }
