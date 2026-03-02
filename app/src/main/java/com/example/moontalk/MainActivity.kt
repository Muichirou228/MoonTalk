package com.example.moontalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moontalk.ui.theme.MoonTalkTheme
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoonTalkTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TestSupabaseConnection()
                }
            }
        }
    }
}

@Composable
fun TestSupabaseConnection() {
    var login by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        login = try {
            SupabaseClient.client.from("users").select { filter {eq("login", "moon")} }
                .decodeSingle<JsonObject>()["login"]?.jsonPrimitive?.content ?: ""
        } catch (e: Exception) { "" }
    }

    Text(text = login,
        modifier = Modifier.padding(top = 16.dp))
}