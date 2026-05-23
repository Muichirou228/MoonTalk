package com.example.moontalk

import android.R
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.moontalk.ui.theme.MoonTalkTheme
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.SignOutScope
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, ExitDetectionService::class.java))
        } else {
            startService(Intent(this, ExitDetectionService::class.java))
        }
        AppState.currentRoomId = null
        setContent {
            MoonTalkTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    var isAuthenticated by remember { mutableStateOf(false) }
                    var scope = rememberCoroutineScope()
                    var isLoading by remember {mutableStateOf(true)}
                    var showRegister by remember {mutableStateOf(false)}
                    var profile by remember {mutableStateOf<Profile?>(null)}
                    var rep = SupabaseRepository()
                    LaunchedEffect(Unit) {
                        val isLoggedIn = rep.isUserLoggedIn()
                        isAuthenticated = isLoggedIn

                        if (isLoggedIn) {
                            val result = rep.getProfile()
                            if (result.isSuccess) {
                                profile = result.getOrNull()
                                Log.d("Chat", "Profile loaded: ${profile?.id}")
                                rep.changeUserSearching(false)
                                rep.changeUserOnline(true)
                            } else {
                                Log.e("Chat", "Failed to load profile: ${result.exceptionOrNull()?.message}")
                                isAuthenticated = false
                            }
                        }
                        isLoading = false
                    }
                    LaunchedEffect(isAuthenticated) {
                        if (isAuthenticated) {
                            profile = rep.getProfile().getOrNull()
                            Log.d("Chat", "Profile in main activity is ${profile?.id}")
                        }
                    }
                    if (isLoading) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF9C27B0))
                        }
                    } else {
                        Crossfade(targetState = Pair(isAuthenticated, showRegister)) { (isAuth, isRegister) ->
                            when {
                                isAuth -> {
                                    Log.d("Chat", "In menu as parametr goes ${profile?.id}")
                                    Menu(profile, {
                                        isAuthenticated = false
                                        scope.launch {
                                            SupabaseClient.client.auth.signOut()
                                        }
                                    },
                                        context = this)
                                }
                                isRegister -> MoonTalkRegistration({isAuthenticated = true},
                                    {showRegister = false})
                                else -> MoonTalkSignIn({isAuthenticated = true},
                                    {showRegister = true})
                            }
                        }
                    }
                }
            }
        }
    }
}
