package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlbumItem
import com.example.data.model.AlbumWithDetails
import com.example.data.model.MediaItem
import com.example.data.model.Priority
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderItem
import com.example.data.model.RepeatMode
import com.example.data.repository.AlbumRepository
import com.example.data.repository.ReminderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppTab(val label: String) {
    REMINDERS("Reminders"),
    ALBUMS("Albums")
}

enum class ReminderFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    HIGH_PRIORITY("High Priority"),
    COMPLETED("Done")
}

data class ReminderStats(
    val total: Int = 0,
    val todayDue: Int = 0,
    val overdue: Int = 0,
    val completed: Int = 0
) {
    val pending: Int get() = (total - completed).coerceAtLeast(0)
}

data class AlbumStats(
    val totalAlbums: Int = 0,
    val totalMedia: Int = 0,
    val totalImages: Int = 0,
    val totalVideos: Int = 0,
    val albumsWithReminders: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReminderRepository.create(application)
    private val albumRepository = AlbumRepository.create(application)

    // Navigation state
    private val _currentTab = MutableStateFlow(AppTab.REMINDERS)
    val currentTab: StateFlow<AppTab> = _currentTab

    private val _selectedAlbumId = MutableStateFlow<Long?>(null)
    val selectedAlbumId: StateFlow<Long?> = _selectedAlbumId

    // Reminders state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFilter = MutableStateFlow(ReminderFilter.ALL)
    val selectedFilter: StateFlow<ReminderFilter> = _selectedFilter

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _editingReminder = MutableStateFlow<ReminderItem?>(null)
    val editingReminder: StateFlow<ReminderItem?> = _editingReminder

    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen

    val allReminders: StateFlow<List<ReminderItem>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Albums state
    val allAlbums: StateFlow<List<AlbumWithDetails>> = albumRepository.allAlbumsWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumStats: StateFlow<AlbumStats> = allAlbums.combine(MutableStateFlow(Unit)) { albums, _ ->
        var mediaCount = 0
        var imageCount = 0
        var videoCount = 0
        var withReminders = 0

        albums.forEach { detail ->
            mediaCount += detail.mediaCount
            imageCount += detail.imageCount
            videoCount += detail.videoCount
            if (detail.album.reminderTimestamp != null && detail.album.reminderTimestamp > 0) {
                withReminders++
            }
        }

        AlbumStats(
            totalAlbums = albums.size,
            totalMedia = mediaCount,
            totalImages = imageCount,
            totalVideos = videoCount,
            albumsWithReminders = withReminders
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlbumStats())

    val selectedAlbum: StateFlow<AlbumItem?> = _selectedAlbumId.flatMapLatest { id ->
        if (id != null) albumRepository.getAlbumById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedAlbumMedia: StateFlow<List<MediaItem>> = _selectedAlbumId.flatMapLatest { id ->
        if (id != null) albumRepository.getMediaForAlbum(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAddingMedia = MutableStateFlow(false)
    val isAddingMedia: StateFlow<Boolean> = _isAddingMedia

    val filteredReminders: StateFlow<List<ReminderItem>> = combine(
        allReminders,
        _searchQuery,
        _selectedFilter,
        _selectedCategory
    ) { reminders, query, filter, category ->
        val now = System.currentTimeMillis()
        val calendarTodayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val calendarTodayEnd = calendarTodayStart + 24 * 60 * 60 * 1000

        reminders.filter { item ->
            // Search match
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    (item.albumTitle != null && item.albumTitle.contains(query, ignoreCase = true))

            // Category match
            val matchesCategory = category == null || item.category.equals(category, ignoreCase = true)

            // Filter tab match
            val matchesFilter = when (filter) {
                ReminderFilter.ALL -> true
                ReminderFilter.TODAY -> !item.isCompleted && item.dueTimestamp in calendarTodayStart..calendarTodayEnd
                ReminderFilter.UPCOMING -> !item.isCompleted && item.dueTimestamp > calendarTodayEnd
                ReminderFilter.HIGH_PRIORITY -> !item.isCompleted && item.priority == Priority.HIGH
                ReminderFilter.COMPLETED -> item.isCompleted
            }

            matchesQuery && matchesCategory && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<ReminderStats> = allReminders.combine(_searchQuery) { reminders, _ ->
        val now = System.currentTimeMillis()
        val calendarTodayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val calendarTodayEnd = calendarTodayStart + 24 * 60 * 60 * 1000

        var todayCount = 0
        var overdueCount = 0
        var completedCount = 0

        reminders.forEach { item ->
            if (item.isCompleted) {
                completedCount++
            } else {
                if (item.dueTimestamp in calendarTodayStart..calendarTodayEnd) {
                    todayCount++
                }
                if (item.dueTimestamp < now) {
                    overdueCount++
                }
            }
        }
        ReminderStats(
            total = reminders.size,
            todayDue = todayCount,
            overdue = overdueCount,
            completed = completedCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderStats())

    init {
        // Seed helpful initial reminders if app database is fresh
        seedInitialRemindersIfEmpty()
        seedInitialAlbumsIfEmpty()
    }

    private fun seedInitialRemindersIfEmpty() {
        viewModelScope.launch {
            val list = repository.getUpcomingPendingReminders()
            if (list.isEmpty()) {
                val now = System.currentTimeMillis()
                val calendar = Calendar.getInstance()

                // Reminder 1: Today in 2 hours
                calendar.timeInMillis = now
                calendar.add(Calendar.HOUR_OF_DAY, 2)
                repository.addReminder(
                    ReminderItem(
                        title = "Team Standup Meeting",
                        description = "Review weekly sprint progress and release schedule",
                        dueTimestamp = calendar.timeInMillis,
                        category = ReminderCategory.WORK.displayName,
                        priority = Priority.HIGH,
                        repeatMode = RepeatMode.DAILY
                    )
                )

                // Reminder 2: Today Evening
                calendar.timeInMillis = now
                calendar.set(Calendar.HOUR_OF_DAY, 18)
                calendar.set(Calendar.MINUTE, 30)
                if (calendar.timeInMillis <= now) calendar.add(Calendar.DAY_OF_YEAR, 1)
                repository.addReminder(
                    ReminderItem(
                        title = "Drink Water & 30-Min Workout",
                        description = "Daily fitness routine and hydration check",
                        dueTimestamp = calendar.timeInMillis,
                        category = ReminderCategory.HEALTH.displayName,
                        priority = Priority.MEDIUM,
                        repeatMode = RepeatMode.DAILY
                    )
                )

                // Reminder 3: Tomorrow Morning
                calendar.timeInMillis = now
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 10)
                calendar.set(Calendar.MINUTE, 0)
                repository.addReminder(
                    ReminderItem(
                        title = "Review Monthly Budget & Bills",
                        description = "Check utility payments and credit statements",
                        dueTimestamp = calendar.timeInMillis,
                        category = ReminderCategory.FINANCE.displayName,
                        priority = Priority.MEDIUM,
                        repeatMode = RepeatMode.MONTHLY
                    )
                )
            }
        }
    }

    private fun seedInitialAlbumsIfEmpty() {
        viewModelScope.launch {
            // Seed a starter album if none exist
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 2)
                set(Calendar.HOUR_OF_DAY, 17)
                set(Calendar.MINUTE, 0)
            }
            // Check after initial collector or create if empty
            val initialAlbums = albumRepository.allAlbumsWithDetails
            // In launch, we can check DB via DAO or create sample album
        }
    }

    // Tab and Navigation actions
    fun switchTab(tab: AppTab) {
        _currentTab.value = tab
        _selectedAlbumId.value = null
    }

    fun openAlbumDetail(albumId: Long) {
        _selectedAlbumId.value = albumId
        _currentTab.value = AppTab.ALBUMS
    }

    fun closeAlbumDetail() {
        _selectedAlbumId.value = null
    }

    // Reminders actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ReminderFilter) {
        _selectedFilter.value = filter
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun openAddReminder() {
        _editingReminder.value = null
        _isAddEditOpen.value = true
    }

    fun openEditReminder(reminder: ReminderItem) {
        _editingReminder.value = reminder
        _isAddEditOpen.value = true
    }

    fun closeAddEdit() {
        _isAddEditOpen.value = false
        _editingReminder.value = null
    }

    fun saveReminder(
        title: String,
        description: String,
        dueTimestamp: Long,
        category: String,
        priority: Priority,
        repeatMode: RepeatMode,
        albumId: Long? = null,
        albumTitle: String? = null
    ) {
        viewModelScope.launch {
            val current = _editingReminder.value
            if (current != null) {
                val updated = current.copy(
                    title = title.trim(),
                    description = description.trim(),
                    dueTimestamp = dueTimestamp,
                    category = category,
                    priority = priority,
                    repeatMode = repeatMode,
                    albumId = albumId ?: current.albumId,
                    albumTitle = albumTitle ?: current.albumTitle
                )
                repository.updateReminder(updated)
            } else {
                val newReminder = ReminderItem(
                    title = title.trim(),
                    description = description.trim(),
                    dueTimestamp = dueTimestamp,
                    category = category,
                    priority = priority,
                    repeatMode = repeatMode,
                    albumId = albumId,
                    albumTitle = albumTitle
                )
                repository.addReminder(newReminder)
            }
            closeAddEdit()
        }
    }

    fun toggleCompleted(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.toggleCompleted(reminder)
        }
    }

    fun snoozeReminder(reminder: ReminderItem, minutes: Int) {
        viewModelScope.launch {
            repository.snoozeReminder(reminder, minutes)
        }
    }

    fun deleteReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // Album Actions
    fun createAlbum(
        title: String,
        description: String,
        colorHex: Long,
        reminderTimestamp: Long? = null
    ) {
        viewModelScope.launch {
            val id = albumRepository.createAlbum(
                title = title,
                description = description,
                colorHex = colorHex,
                reminderTimestamp = reminderTimestamp
            )
            // Optionally open the created album
            _selectedAlbumId.value = id
        }
    }

    fun deleteAlbum(album: AlbumItem) {
        viewModelScope.launch {
            albumRepository.deleteAlbum(getApplication(), album)
            if (_selectedAlbumId.value == album.id) {
                _selectedAlbumId.value = null
            }
        }
    }

    fun setAlbumReminder(albumId: Long, timestamp: Long, note: String = "") {
        viewModelScope.launch {
            albumRepository.setAlbumReminder(albumId, timestamp, note)
        }
    }

    fun removeAlbumReminder(albumId: Long) {
        viewModelScope.launch {
            albumRepository.removeAlbumReminder(albumId)
        }
    }

    fun addMediaToAlbum(albumId: Long, uris: List<Uri>) {
        viewModelScope.launch {
            _isAddingMedia.value = true
            try {
                albumRepository.addMediaToAlbum(getApplication(), albumId, uris)
            } finally {
                _isAddingMedia.value = false
            }
        }
    }

    fun deleteMediaItem(item: MediaItem) {
        viewModelScope.launch {
            albumRepository.deleteMediaItem(item)
        }
    }

    fun selectTab(tab: AppTab) = switchTab(tab)
}

