package com.vontext.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vontext.ui.screens.HistoryScreen
import com.vontext.ui.screens.HomeScreen
import com.vontext.ui.screens.SettingsScreen
import com.vontext.ui.theme.BlueFAB

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        "home",
        "Inicio",
        Icons.Filled.Home,
        Icons.Outlined.Home
    )
    object History : BottomNavItem(
        "history",
        "Historial",
        Icons.Filled.VideoLibrary,
        Icons.Outlined.VideoLibrary
    )
    object Settings : BottomNavItem(
        "settings",
        "Configuración",
        Icons.Filled.Settings,
        Icons.Outlined.Settings
    )
}

@Composable
fun VontextApp() {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val navItems = listOf(BottomNavItem.Home, BottomNavItem.History, BottomNavItem.Settings)

    val selectedVideos = remember { mutableStateListOf<Uri>() }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            if (uri !in selectedVideos) {
                selectedVideos.add(uri)
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { videoPicker.launch("video/*") },
                    containerColor = BlueFAB,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar videos",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(
                modifier = Modifier.padding(padding),
                selectedVideos = selectedVideos,
                onNavigateToProcessing = { videos, processTogether, interval, notes ->
                    // TODO: Iniciar procesamiento
                }
            )
            1 -> HistoryScreen(
                onNavigateBack = { selectedTab = 0 }
            )
            2 -> SettingsScreen(
                onNavigateBack = { selectedTab = 0 }
            )
        }
    }
}
