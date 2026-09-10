package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AlbumItem
import com.example.ui.AlbumStats
import com.example.ui.ReminderViewModel
import com.example.ui.components.AlbumCard
import com.example.ui.components.CreateAlbumDialog
import com.example.ui.components.SetAlbumReminderDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier
) {
    val albums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val albumStats by viewModel.albumStats.collectAsStateWithLifecycle()

    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var albumToSetReminder by remember { mutableStateOf<AlbumItem?>(null) }
    var albumToDelete by remember { mutableStateOf<AlbumItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoAlbum,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Memory Albums",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateAlbumDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Album") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("create_album_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats Overview Card
            item {
                AlbumOverviewHeader(stats = albumStats)
            }

            // Albums list
            if (albums.isEmpty()) {
                item {
                    EmptyAlbumsView(
                        onCreateNew = { showCreateAlbumDialog = true }
                    )
                }
            } else {
                items(
                    items = albums,
                    key = { it.album.id }
                ) { item ->
                    AlbumCard(
                        albumWithDetails = item,
                        onClick = { viewModel.openAlbumDetail(item.album.id) },
                        onSetReminder = { albumToSetReminder = item.album },
                        onDelete = { albumToDelete = item.album }
                    )
                }
            }
        }
    }

    // Create Album Dialog
    if (showCreateAlbumDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateAlbumDialog = false },
            onSave = { title, desc, colorHex, reminderTs ->
                viewModel.createAlbum(title, desc, colorHex, reminderTs)
                showCreateAlbumDialog = false
            }
        )
    }

    // Set/Edit Album Reminder Dialog
    albumToSetReminder?.let { album ->
        SetAlbumReminderDialog(
            album = album,
            onDismiss = { albumToSetReminder = null },
            onSaveReminder = { ts, note ->
                viewModel.setAlbumReminder(album.id, ts, note)
                albumToSetReminder = null
            },
            onRemoveReminder = {
                viewModel.removeAlbumReminder(album.id)
                albumToSetReminder = null
            }
        )
    }

    // Delete Album Confirmation Dialog
    albumToDelete?.let { album ->
        AlertDialog(
            onDismissRequest = { albumToDelete = null },
            title = { Text("Delete Album?") },
            text = { Text("Are you sure you want to delete \"${album.title}\"? All photos and videos inside will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAlbum(album)
                        albumToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { albumToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AlbumOverviewHeader(stats: AlbumStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumStatItem(
                count = stats.totalAlbums,
                label = "Albums",
                icon = Icons.Default.PhotoLibrary,
                tint = MaterialTheme.colorScheme.primary
            )

            AlbumStatItem(
                count = stats.totalImages,
                label = "Photos",
                icon = Icons.Default.Image,
                tint = Color(0xFF0288D1)
            )

            AlbumStatItem(
                count = stats.totalVideos,
                label = "Videos",
                icon = Icons.Default.Videocam,
                tint = Color(0xFFE65100)
            )

            AlbumStatItem(
                count = stats.albumsWithReminders,
                label = "Reminders",
                icon = Icons.Default.Alarm,
                tint = Color(0xFF7B1FA2)
            )
        }
    }
}

@Composable
private fun AlbumStatItem(
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.14f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyAlbumsView(
    onCreateNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Albums Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Create albums to store your memorable photos, record video moments, and schedule timely reminders.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onCreateNew,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("empty_create_album_btn")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create First Album")
        }
    }
}
