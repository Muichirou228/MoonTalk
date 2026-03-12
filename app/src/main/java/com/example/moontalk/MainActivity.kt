package com.example.moontalk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.moontalk.ui.theme.MoonTalkTheme
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, ExitDetectionService::class.java))
        enableEdgeToEdge()
        setContent {
            MoonTalkTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    var isAuthenticated by remember { mutableStateOf(false) }
                    Crossfade(targetState = isAuthenticated) { isAuth ->
                        if (isAuth) {
                            FindCompanions()
                        } else {
                            MoonTalkSignIn({
                                isAuthenticated = true
                            })
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun MoonTalkSignIn(onAuthSuccess: () -> Unit) {
    var email by remember {mutableStateOf("")}
    var password by remember {mutableStateOf("")}
    var isLoading by remember {mutableStateOf(false)}
    var scope = rememberCoroutineScope()
    var showAlert by remember {mutableStateOf(false)}
    var alertMessage by remember {mutableStateOf("")}

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
                        val profile = result.getOrNull()
//                      alertMessage = profile!!.username
//                      showAlert = true
                        withContext(Dispatchers.Main) {
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