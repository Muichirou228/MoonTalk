package com.example.moontalk

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MoonTalkSignIn(onAuthSuccess: () -> Unit,
                   onNavigateToRegister: () -> Unit) {
    var email by remember {mutableStateOf("")}
    var password by remember {mutableStateOf("")}
    var isLoading by remember {mutableStateOf(false)}
    var scope = rememberCoroutineScope()
    var showAlert by remember {mutableStateOf(false)}
    var alertMessage by remember {mutableStateOf("")}
    Column(modifier = Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.End) {
        Text("Нет аккаунта?", color = Color.White)
        Text("Зарегистрироваться", fontWeight = FontWeight.ExtraBold, color = Color.White, textDecoration = TextDecoration.Underline, fontSize = 20.sp, modifier = Modifier.clickable(true){
            onNavigateToRegister()
        })
    }
    Column (modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center){
        Row (modifier = Modifier.padding(bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text (text = "ВХОД", fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
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
        Spacer (modifier = Modifier.height(30.dp))
        Button(onClick = {
            scope.launch {
                try {
                    var rep = SupabaseRepository()
                    isLoading = true;
                    var result = rep.signIn(email, password)
                    if (result.isSuccess) {
//                        alertMessage = profile!!.username
//                        showAlert = true
                        withContext(Dispatchers.Main) {
                            //delay(1000) //!!!!!!!!
                            isLoading = false
                            onAuthSuccess()
                        }
                    } else {
                        alertMessage = "Неправильно введены данные"
                        showAlert = true
                        isLoading = false
                    }
                } catch (e: Exception) {
                    alertMessage = "${e.message}"
                    showAlert = true
                    isLoading = false
                }
            }
        }, modifier = Modifier.width(200.dp).height(70.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9C27B0),
                contentColor = Color.White
            ),
            enabled = email != "" && password != "" && !isLoading) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else Text("Войти", fontSize = 26.sp)
        }
    }
    if (showAlert) {
        MaterialAlert(alertMessage, onDismiss = {showAlert = false})
    }
}