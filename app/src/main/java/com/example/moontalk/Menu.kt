package com.example.moontalk

import android.content.Context
import android.util.Log
import androidx.annotation.NavigationRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun Menu(profile: Profile?, onLogOut: () -> Unit, context: Context) {
    var selectedTab by remember { mutableStateOf(0) }
    var isSearching by remember {mutableStateOf(false)}
    LaunchedEffect(Unit) {
        Log.d("Chat", "Profile which is in menu is ${profile?.id}")
    }
    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = !isSearching,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                NavigationBar(containerColor = Color.Black) {  // ← добавить NavigationBar
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { if (!isSearching) selectedTab = 0 },
                        icon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Поиск",
                                tint = if (selectedTab == 0) Color(0xFF9C27B0) else Color.White
                            )
                        },
                        label = {
                            Text(
                                "Поиск",
                                color = if (selectedTab == 0) Color(0xFF9C27B0) else Color.White
                            )
                        },
                        enabled = !isSearching
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = {
                            if (!isSearching) selectedTab = 1
                        },
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Фидбэк",
                                tint = if (selectedTab == 1) Color(0xFF9C27B0) else Color.White
                            )
                        },
                        label = {
                            Text(
                                "Фидбэки",
                                color = if (selectedTab == 1) Color(0xFF9C27B0) else Color.White
                            )
                        },
                        enabled = !isSearching
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = {
                            if (!isSearching) selectedTab = 2
                        },
                        icon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Профиль",
                                tint = if (selectedTab == 2) Color(0xFF9C27B0) else Color.White
                            )
                        },
                        label = {
                            Text(
                                "Профиль",
                                color = if (selectedTab == 2) Color(0xFF9C27B0) else Color.White
                            )
                        },
                        enabled = !isSearching
                    )
                }
            }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            when (selectedTab) {
                0 -> FindCompanions(
                    initialProfile = profile,
                    onSearchingChanged = { searching -> isSearching = searching },
                    searchStatus = isSearching,
                    context = context
                )
                1 -> FeedbackScreen()
                2 -> Account(profile, onLogOut)
            }
        }
    }
}