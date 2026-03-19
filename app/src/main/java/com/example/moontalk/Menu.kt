package com.example.moontalk

import androidx.annotation.NavigationRes
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
fun Menu(profile: Profile?) {
    var selectedTab by remember { mutableStateOf(0) }
    var rep = SupabaseRepository()
    var scope = rememberCoroutineScope()
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
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
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        scope.launch {
                            rep.changeUserSearching(false)
                        }
                        selectedTab = 1 },
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
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        scope.launch {
                            rep.changeUserSearching(false)
                        }
                        selectedTab = 2 },
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Фидбэк",
                            tint = if (selectedTab == 2) Color(0xFF9C27B0) else Color.White
                        )
                    },
                    label = {
                        Text(
                            "Профиль",
                            color = if (selectedTab == 2) Color(0xFF9C27B0) else Color.White
                        )
                    }
                )
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
                0 -> FindCompanions()
                1 -> Feedback()
                2 -> Account(profile)
            }
        }
    }
}