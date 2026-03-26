package com.example.moontalk

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
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Account(profile: Profile?, onLogout: () -> Unit) {
    val rep = SupabaseRepository()
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Состояние для выбранного языка
    var selectedLanguage by remember { mutableStateOf(profile?.learning_language ?: "en") }
    var isUpdatingLanguage by remember { mutableStateOf(false) }

    // Список доступных языков
    val languages = listOf(
        "en" to "🇬🇧 English",
        "ru" to "🇷🇺 Русский",
        "es" to "🇪🇸 Español",
        "fr" to "🇫🇷 Français",
        "de" to "🇩🇪 Deutsch",
        "it" to "🇮🇹 Italiano",
        "ja" to "🇯🇵 日本語",
        "ko" to "🇰🇷 한국어",
        "zh" to "🇨🇳 中文"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Верхний бар
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Левая часть: аватар + имя
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Иконка-заглушка
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF9C27B0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile?.username?.take(1)?.uppercase() ?: "?",
                        fontSize = 28.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Имя пользователя крупно
                Text(
                    text = profile?.username ?: "Неизвестно",
                    fontSize = 22.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // Правая часть: кнопка выхода
            IconButton(
                onClick = { showLogoutDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Выйти",
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Основной контент
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Карточка с языком
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
                    // Заголовок
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

                    // Выпадающий список
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = {}
                    ) {
                        OutlinedTextField(
                            value = languages.find { it.first == selectedLanguage }?.second ?: selectedLanguage,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF9C27B0),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        DropdownMenu(
                            expanded = false,
                            onDismissRequest = {},
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            languages.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name, color = Color.White) },
                                    onClick = {
                                        selectedLanguage = code
                                        isUpdatingLanguage = true
                                        scope.launch {
                                            // Обновляем язык в БД
                                            rep.updateUserLanguage(code)
                                            isUpdatingLanguage = false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Индикатор загрузки
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

            Spacer(modifier = Modifier.height(24.dp))

            // Статистика
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    value = "0",
                    label = "Чатов"
                )
                StatCard(
                    value = "0",
                    label = "Фидбеков"
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }

    // Диалог подтверждения выхода
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выход", color = Color.White) },
            text = { Text("Вы уверены, что хотите выйти?", color = Color.White.copy(alpha = 0.7f)) },
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
            }
        )
    }
}

@Composable
fun StatCard(value: String, label: String) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 32.sp,
                color = Color(0xFF9C27B0),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}