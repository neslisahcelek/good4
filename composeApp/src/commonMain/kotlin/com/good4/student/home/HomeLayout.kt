package com.good4.student.home

import com.good4.user.domain.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class HomeShortcut(val id: String, val defaultVisible: Boolean) {
    COMMUNITIES("communities", true),
    CLASS_SCHEDULE("classSchedule", true),
    CAMPUS_MAP("campusMap", true),
    ACADEMIC_CALENDAR("academicCalendar", true),
    SUSPENDED_MEALS("suspendedMeals", true),
    CAMPUS_CLOSET("campusCloset", true),
    TOP_UP("topUp", false),
    TENNIS("tennis", false),
    PHONE_NUMBERS("phoneNumbers", false),
    FEEDBACK("feedback", false);

    val definition get() = HomeShortcutDefinition(id, defaultVisible)
}

data class HomeShortcutDefinition(val id: String, val defaultVisible: Boolean)

fun availableHomeShortcuts(
    role: UserRole,
    suspendedMealsEnabled: Boolean,
    campusClosetEnabled: Boolean = false
): List<HomeShortcut> =
    if (role != UserRole.STUDENT) emptyList()
    else HomeShortcut.entries.filter {
        (it != HomeShortcut.SUSPENDED_MEALS || suspendedMealsEnabled)
            && (it != HomeShortcut.CAMPUS_CLOSET || campusClosetEnabled)
    }

@Serializable
data class HomeLayout(val visible: List<String> = emptyList(), val hidden: List<String> = emptyList()) {
    /** Remove stale/duplicate IDs, then append newly introduced shortcuts to their default section. */
    fun resolve(definitions: List<HomeShortcutDefinition>): HomeLayout {
        val allowed = definitions.map { it.id }.toSet()
        val seen = mutableSetOf<String>()
        val shown = visible.filter { it in allowed && seen.add(it) }.toMutableList()
        val concealed = hidden.filter { it in allowed && seen.add(it) }.toMutableList()
        definitions.filter { seen.add(it.id) }.forEach {
            if (it.defaultVisible) shown.add(it.id) else concealed.add(it.id)
        }
        // Corrupt/old storage or a feature switch must never leave an available home empty.
        if (shown.isEmpty() && concealed.isNotEmpty()) shown.add(concealed.removeAt(0))
        return HomeLayout(shown, concealed)
    }

    /** The index is in the destination section AFTER removing the moving shortcut. */
    fun move(id: String, toVisible: Boolean, index: Int): HomeLayout? {
        if (id !in visible && id !in hidden) return null
        if (!toVisible && id in visible && visible.size == 1) return null
        val shown = visible.filterNot { it == id }.toMutableList()
        val concealed = hidden.filterNot { it == id }.toMutableList()
        val destination = if (toVisible) shown else concealed
        destination.add(index.coerceIn(0, destination.size), id)
        return HomeLayout(shown, concealed)
    }
}

interface HomeLayoutStorage {
    fun read(uid: String): String?
    fun write(uid: String, value: String)
}

expect fun loadHomeLayout(uid: String): String?
expect fun saveHomeLayout(uid: String, value: String)

class DeviceHomeLayoutStorage : HomeLayoutStorage {
    override fun read(uid: String) = loadHomeLayout(uid)
    override fun write(uid: String, value: String) = saveHomeLayout(uid, value)
}

/** Shared by the home and editor view models; each write refreshes both without a network request. */
class HomeLayoutStore(private val storage: HomeLayoutStorage) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _revision = MutableStateFlow(0L)
    val revision = _revision.asStateFlow()

    fun read(uid: String, definitions: List<HomeShortcutDefinition>): HomeLayout {
        if (uid.isBlank()) return HomeLayout()
        val saved = storage.read(uid)?.let { raw ->
            runCatching { json.decodeFromString<HomeLayout>(raw) }.getOrNull()
        } ?: HomeLayout()
        return saved.resolve(definitions)
    }

    fun write(uid: String, layout: HomeLayout) {
        if (uid.isBlank()) return
        storage.write(uid, json.encodeToString(layout))
        _revision.value += 1
    }
}
