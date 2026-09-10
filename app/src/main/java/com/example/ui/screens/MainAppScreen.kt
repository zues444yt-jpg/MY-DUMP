package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.ReminderViewModel

@Composable
fun MainAppScreen(
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedAlbumId by viewModel.selectedAlbumId.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val albumStats by viewModel.albumStats.collectAsStateWithLifecycle()

    // Handle system back button when inside an album detail
    if (selectedAlbumId != null) {
        BackHandler {
            viewModel.closeAlbumDetail()
        }
    }

    if (selectedAlbumId != null && selectedAlbum != null) {
        // Fullscreen Album Detail view
        AlbumDetailScreen(
            viewModel = viewModel,
            album = selectedAlbum!!,
            onBack = { viewModel.closeAlbumDetail() }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = MaterialTheme.colorScheme.surfaceTint.let { 3.dp }
                ) {
                    // Reminders Tab
                    NavigationBarItem(
                        selected = currentTab == AppTab.REMINDERS,
                        onClick = { viewModel.selectTab(AppTab.REMINDERS) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (stats.pending > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text("${stats.pending}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentTab == AppTab.REMINDERS) {
                                        Icons.Filled.Notifications
                                    } else {
                                        Icons.Outlined.Notifications
                                    },
                                    contentDescription = "Reminders"
                                )
                            }
                        },
                        label = {
                            Text(
                                "Reminders",
                                fontWeight = if (currentTab == AppTab.REMINDERS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_reminders")
                    )

                    // Albums Tab
                    NavigationBarItem(
                        selected = currentTab == AppTab.ALBUMS,
                        onClick = { viewModel.selectTab(AppTab.ALBUMS) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (albumStats.totalAlbums > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ) {
                                            Text("${albumStats.totalAlbums}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentTab == AppTab.ALBUMS) {
                                        Icons.Filled.PhotoLibrary
                                    } else {
                                        Icons.Outlined.PhotoLibrary
                                    },
                                    contentDescription = "Albums"
                                )
                            }
                        },
                        label = {
                            Text(
                                "Albums",
                                fontWeight = if (currentTab == AppTab.ALBUMS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_albums")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                when (currentTab) {
                    AppTab.REMINDERS -> {
                        ReminderListScreen(
                            viewModel = viewModel,
                            onOpenAlbum = { albumId ->
                                viewModel.openAlbumDetail(albumId)
                            }
                        )
                    }
                    AppTab.ALBUMS -> {
                        AlbumsScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
