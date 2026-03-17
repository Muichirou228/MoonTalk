package com.example.moontalk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MoonTalkRegistration(onRegisterSuccess: () -> Unit,
                         onNavigateToLogin : () -> Unit) {
    val languages = listOf(
        "Английский", "Испанский", "Французский", "Немецкий",
        "Итальянский", "Португальский", "Китайский", "Японский",
        "Корейский"
    )
    var email by remember {mutableStateOf("")}
    var password by remember {mutableStateOf("")}
    var userName by remember {mutableStateOf("")}
    var selectedLanguage by remember {mutableStateOf("")}
    var isLoading by remember {mutableStateOf(false)}
    var isDropMenuDropped by remember {mutableStateOf(false)}

    var scope = rememberCoroutineScope()
    var showAlert by remember {mutableStateOf(false)}
    var alertMessage by remember {mutableStateOf("")}
    Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.End) {
        Text("Уже есть аккаунт?", color = Color.White)
        Text("Войти", fontWeight = FontWeight.ExtraBold, color = Color.White, textDecoration = TextDecoration.Underline, fontSize = 20.sp, modifier = Modifier.clickable(true){
            onNavigateToLogin()
        })
    }
    Column (modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center){
        Row (modifier = Modifier.padding(bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text (text = "РЕГИСТРАЦИЯ", fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(modifier = Modifier.fillMaxWidth().height(56.dp)
            .border(color = Color.White, width = 3.dp)
            ,
            onValueChange = {newText -> userName = newText},
            value = userName,
            textStyle = TextStyle(color = Color.White),
            placeholder = {Text(text = "Никнейм...", textAlign = TextAlign.Center, color = Color.White)})
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(modifier = Modifier.fillMaxWidth().height(56.dp)
            .border(color = Color.White, width = 3.dp)
            ,
            onValueChange = {newText -> email = newText},
            value = email,
            textStyle = TextStyle(color = Color.White),
            placeholder = {Text(text = "Почта...", textAlign = TextAlign.Center, color = Color.White)})
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(modifier = Modifier.fillMaxWidth().height(56.dp)
            .border(color = Color.White, width = 3.dp)
            ,
            onValueChange = {newText -> password = newText},
            value = password,
            textStyle = TextStyle(color = Color.White),
            placeholder = {Text(text = "Пароль...", textAlign = TextAlign.Center, color = Color.White)})
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(modifier = Modifier.fillMaxWidth()
                .height(56.dp)
                .border(color = Color.White, width = 3.dp),
                onValueChange = {},
                value = selectedLanguage,
                readOnly = true,
                textStyle = TextStyle(color = Color.White),
                placeholder = {Text(text = "Изучаемый язык...", textAlign = TextAlign.Center, color = Color.White)})
            Box (modifier = Modifier.matchParentSize().clickable(true) {
                isDropMenuDropped = true
            })
            DropdownMenu(expanded = isDropMenuDropped,
                onDismissRequest = {isDropMenuDropped = false},
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E))) {
                languages.forEach { language ->
                    DropdownMenuItem(text = {Text(text = language, color = Color.White)}, onClick = {
                        selectedLanguage = language
                        isDropMenuDropped = false
                    })
                }
            }
        }

        Spacer (modifier = Modifier.height(30.dp))
        Button(onClick = {
            scope.launch {
                try {
                    var rep = SupabaseRepository()
                    isLoading = true;
                    var result = rep.register(userName, email, password, selectedLanguage)
                    if (result.isSuccess) {
                        alertMessage = "Профиль добавлен, производится вход..."
                        showAlert = true
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            delay(2000)
                            onRegisterSuccess()
                        }
                    } else {
                        alertMessage = when {
                            alertMessage.contains("User already") -> "Пользователь с такой почтой уже зарегистрирован"
                            alertMessage.contains("email rate limit") -> "Слишком много попыток, попробуйте через 5 минут"
                            alertMessage.contains("invalid email") -> "Некорректный формат email"
                            alertMessage.contains("weak password") -> "Пароль слишком слабый"
                            alertMessage.contains("duplicate") && alertMessage.contains("username") -> "Никнейм уже занят"
                            alertMessage.contains("network") -> "Проблема с интернетом"
                            alertMessage.contains("timeout") -> "Сервер не отвечает"
                            else -> result.exceptionOrNull()?.message?:"Ошибка"
                        }
                        showAlert = true
                        isLoading = false
                    }
                } catch (e: Exception) {
                    alertMessage = "${e.message}"
                    showAlert = true
                    isLoading = false
                }
            }
        }, modifier = Modifier.width(350.dp).height(70.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9C27B0),
                contentColor = Color.White
            ),
            enabled = email != "" && password != "" && !isLoading && userName != "" && selectedLanguage != "") {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else Text("Зарегистрироваться", fontSize = 26.sp)
        }
    }
    if (showAlert) {
        MaterialAlert(alertMessage, onDismiss = {showAlert = false})
    }
}