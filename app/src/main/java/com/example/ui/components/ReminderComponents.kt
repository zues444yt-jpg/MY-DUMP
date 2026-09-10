package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderItem
import com.example.data.model.RepeatMode
import com.example.ui.ReminderStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReminderStatsHeader(
    stats: ReminderStats,
    modifier: Modifier = Modifier
) {
    val completionRatio = if (stats.total > 0) stats.completed.toFloat() / stats.total else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stats_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${stats.completed} of ${stats.total} reminders completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${(completionRatio * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { completionRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBadge(
                    label = "Total",
                    count = stats.total,
                    color = MaterialTheme.colorScheme.primary
                )
                StatBadge(
                    label = "Today",
                    count = stats.todayDue,
                    color = Color(0xFF0288D1)
                )
                StatBadge(
                    label = "Overdue",
                    count = stats.overdue,
                    color = Color(0xFFD32F2F)
                )
                StatBadge(
                    label = "Done",
                    count = stats.completed,
                    color = Color(0xFF388E3C)
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.9f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderCard(
    reminder: ReminderItem,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSnooze: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onOpenAlbum: ((Long) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val isOverdue = reminder.isOverdue
    val category = ReminderCategory.fromString(reminder.category)

    val cardBorder = if (isOverdue) {
        BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f))
    } else if (reminder.isCompleted) {
        BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reminder_item_${reminder.id}"),
        shape = RoundedCornerShape(16.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else if (isOverdue) {
                Color(0xFFFFF1F0)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (reminder.isCompleted) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkmark button
            IconButton(
                onClick = onToggleCompleted,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 4.dp)
                    .testTag("complete_checkbox_${reminder.id}")
            ) {
                Icon(
                    imageVector = if (reminder.isCompleted) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = if (reminder.isCompleted) "Mark as incomplete" else "Mark as completed",
                    tint = if (reminder.isCompleted) {
                        Color(0xFF388E3C)
                    } else if (isOverdue) {
                        Color(0xFFD32F2F)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            // Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                // Title
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (reminder.isCompleted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (reminder.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (reminder.isCompleted) 0.5f else 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Chips: Time, Category, Priority, Repeat
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Due Time Badge
                    TimeBadge(
                        timestamp = reminder.dueTimestamp,
                        isCompleted = reminder.isCompleted,
                        isOverdue = isOverdue
                    )

                    // Category Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(category.colorHex).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(category.colorHex),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Priority Badge (only if High or Low)
                    if (reminder.priority != Priority.MEDIUM && !reminder.isCompleted) {
                        val pColor = if (reminder.priority == Priority.HIGH) Color(0xFFD32F2F) else Color(0xFF757575)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = pColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${reminder.priority.label} Priority",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = pColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Repeat indicator
                    if (reminder.repeatMode != RepeatMode.NONE) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Repeat",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = reminder.repeatMode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Album linked badge
                    if (reminder.albumId != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD81B60).copy(alpha = 0.12f),
                            modifier = if (onOpenAlbum != null) {
                                Modifier.clickable { onOpenAlbum(reminder.albumId) }
                            } else Modifier
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFFD81B60),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = reminder.albumTitle?.let { "Album: $it" } ?: "Album",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFD81B60)
                                )
                            }
                        }
                    }
                }
            }

            // More actions menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (!reminder.isCompleted) {
                        DropdownMenuItem(
                            text = { Text("Snooze 15 minutes") },
                            onClick = {
                                showMenu = false
                                onSnooze(15)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Alarm, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Snooze 1 hour") },
                            onClick = {
                                showMenu = false
                                onSnooze(60)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Snooze until tomorrow") },
                            onClick = {
                                showMenu = false
                                onSnooze(24 * 60)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Edit Reminder") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFD32F2F)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeBadge(
    timestamp: Long,
    isCompleted: Boolean,
    isOverdue: Boolean
) {
    val formatted = formatRelativeDateTime(timestamp)
    val badgeColor = if (isCompleted) {
        Color.Gray
    } else if (isOverdue) {
        Color(0xFFD32F2F)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = badgeColor.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isOverdue && !isCompleted) Icons.Outlined.Warning else Icons.Default.Schedule,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isOverdue && !isCompleted) "Overdue: $formatted" else formatted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = badgeColor
            )
        }
    }
}

fun formatRelativeDateTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }

    val sameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = tomorrow.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            tomorrow.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = timeFormat.format(Date(timestamp))

    return when {
        sameDay -> "Today, $timeStr"
        isTomorrow -> "Tomorrow, $timeStr"
        else -> {
            val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
