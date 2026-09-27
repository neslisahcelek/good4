package com.good4.student.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.SurfaceCanvasWarm
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.student.home.HomeLayout
import com.good4.student.home.HomeLayoutViewModel
import com.good4.student.home.HomeShortcut
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private const val VISIBLE_HEADER = "section-visible"
private const val HIDDEN_HEADER = "section-hidden"
private const val EMPTY_HIDDEN = "empty-hidden"
private const val LAST_SHORTCUT_WARNING = "Ana sayfada en az bir kısayol kalmalı."

private class ShortcutDragState {
    var id by mutableStateOf<String?>(null)
    var centerY by mutableFloatStateOf(0f)
    var rejected by mutableStateOf(false)
    var viewport: LayoutCoordinates? = null
}

private class ShortcutHandleCoordinates {
    var value: LayoutCoordinates? = null
}

@Composable
fun EditHomeScreen(
    onBack: () -> Unit,
    communityManager: Boolean = false,
    viewModel: HomeLayoutViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val drag = remember { ShortcutDragState() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val edge = with(density) { 72.dp.toPx() }
    val maxScroll = with(density) { 12.dp.toPx() }
    var confirmReset by remember { mutableStateOf(false) }

    fun move(id: String, shown: Boolean, index: Int): Boolean {
        val allowed = viewModel.move(id, shown, index)
        if (!allowed) {
            scope.launch { snackbar.showSnackbar(LAST_SHORTCUT_WARNING) }
        }
        return allowed
    }

    fun updateDragTarget() {
        val id = drag.id ?: return
        val layout = viewModel.state.value.layout
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            drag.centerY >= it.offset && drag.centerY < it.offset + it.size
        } ?: return
        val key = target.key as? String ?: return
        val shown: Boolean
        val index: Int
        when (key) {
            VISIBLE_HEADER -> { shown = true; index = 0 }
            HIDDEN_HEADER, EMPTY_HIDDEN -> { shown = false; index = 0 }
            else -> {
                if (key == id) return
                shown = key in layout.visible
                val section = (if (shown) layout.visible else layout.hidden).filterNot { it == id }
                val targetIndex = section.indexOf(key)
                if (targetIndex == -1) return
                index = targetIndex + if (drag.centerY > target.offset + target.size / 2f) 1 else 0
            }
        }
        if (!viewModel.move(id, shown, index)) drag.rejected = true
    }

    // The pointer stays in viewport coordinates while the list scrolls under it.
    LaunchedEffect(drag.id) {
        while (drag.id != null) {
            val info = listState.layoutInfo
            val top = info.viewportStartOffset + edge
            val bottom = info.viewportEndOffset - edge
            val scroll = when {
                drag.centerY < top -> -maxScroll * ((top - drag.centerY) / edge).coerceIn(0f, 1f)
                drag.centerY > bottom -> maxScroll * ((drag.centerY - bottom) / edge).coerceIn(0f, 1f)
                else -> 0f
            }
            if (scroll != 0f) {
                listState.scrollBy(scroll)
                updateDragTarget()
            }
            delay(16)
        }
    }

    LaunchedEffect(state.uid) { drag.id = null }

    Good4Scaffold(
        topBar = {
            Good4TopBar(
                title = "Sayfanı Düzenle",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    TextButton(onClick = { confirmReset = true }, enabled = state.shortcuts.isNotEmpty()) {
                        Text("Varsayılana dön", fontSize = 12.sp)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(SurfaceCanvasWarm).padding(padding)
                .onGloballyPositioned { drag.viewport = it },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // One keyed item interval keeps the row's pointer recognizer alive when it
            // crosses the section divider. Separate visible/hidden items blocks cancel it.
            val keys = listOf(VISIBLE_HEADER) + state.layout.visible + HIDDEN_HEADER +
                state.layout.hidden.ifEmpty { listOf(EMPTY_HIDDEN) }
            items(keys, key = { it }) { key ->
                when (key) {
                    VISIBLE_HEADER -> ShortcutSectionHeader("Ana sayfada", "Sıralamak için sağdaki tutamağı sürükle.")
                    HIDDEN_HEADER -> ShortcutSectionHeader("Menü", "Bu kısayolları + ile veya sürükleyerek ekle.")
                    EMPTY_HIDDEN -> Text(
                        "Bu bölümde kısayol yok. Kısayolları buraya sürükleyebilirsin.", color = TextSecondary,
                        fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                    else -> {
                        val shortcut = state.shortcuts.first { it.id == key }
                        ShortcutEditorRow(
                            shortcut, communityManager, key in state.layout.visible, state.layout, listState, drag,
                            onMove = ::move,
                            onDragStart = { pointerY ->
                                drag.centerY = pointerY
                                drag.rejected = false
                                drag.id = key
                            },
                            onDrag = { pointerY -> drag.centerY = pointerY; updateDragTarget() },
                            onDrop = {
                                drag.id = null
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (drag.rejected) scope.launch { snackbar.showSnackbar(LAST_SHORTCUT_WARNING) }
                            }
                        )
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Varsayılan düzene dön?") },
            text = { Text("Kısayolların sırası ve görünürlüğü başlangıç düzenine dönecek.") },
            confirmButton = {
                TextButton(onClick = { viewModel.reset(); confirmReset = false }) { Text("Varsayılana dön") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Vazgeç") } }
        )
    }
}

@Composable
private fun ShortcutSectionHeader(title: String, subtitle: String) {
    Column(Modifier.padding(top = 12.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(subtitle, fontSize = 13.sp, color = TextSecondary)
    }
}

@Composable
private fun ShortcutEditorRow(
    shortcut: HomeShortcut,
    communityManager: Boolean,
    shown: Boolean,
    layout: HomeLayout,
    listState: LazyListState,
    drag: ShortcutDragState,
    onMove: (String, Boolean, Int) -> Boolean,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDrop: () -> Unit
) {
    val appearance = shortcut.appearance(communityManager)
    val section = if (shown) layout.visible else layout.hidden
    val index = section.indexOf(shortcut.id)
    val active = drag.id == shortcut.id
    val handle = remember { ShortcutHandleCoordinates() }
    // Keep the recognizer alive across reorder recompositions and section changes.
    val start by rememberUpdatedState(onDragStart)
    val move by rememberUpdatedState(onDrag)
    val drop by rememberUpdatedState(onDrop)

    Surface(
        modifier = Modifier.fillMaxWidth().zIndex(if (active) 1f else 0f)
            .graphicsLayer {
                if (active) {
                    val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == shortcut.id }
                    translationY = info?.let { drag.centerY - (it.offset + it.size / 2f) } ?: 0f
                    scaleX = 1.02f
                    scaleY = 1.02f
                }
            }
            .semantics {
                contentDescription = appearance.title
                customActions = buildList {
                    if (index > 0) add(CustomAccessibilityAction("Yukarı taşı") { onMove(shortcut.id, shown, index - 1) })
                    if (index < section.lastIndex) add(CustomAccessibilityAction("Aşağı taşı") { onMove(shortcut.id, shown, index + 1) })
                    add(CustomAccessibilityAction(if (shown) "Gizle" else "Ana sayfaya ekle") {
                        onMove(shortcut.id, !shown, if (shown) layout.hidden.size else layout.visible.size)
                    })
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = if (active) 8.dp else 1.dp
    ) {
        Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = appearance.accent.copy(alpha = .2f),
                border = BorderStroke(1.dp, appearance.accent.copy(alpha = .65f))) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    Icon(appearance.icon, null, tint = appearance.accent, modifier = Modifier.size(25.dp))
                }
            }
            Text(appearance.title, modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            IconButton(onClick = {
                onMove(shortcut.id, !shown, if (shown) layout.hidden.size else layout.visible.size)
            }) {
                Icon(if (shown) Icons.Outlined.VisibilityOff else Icons.Outlined.Add,
                    if (shown) "${appearance.title} kısayolunu gizle" else "${appearance.title} kısayolunu ana sayfaya ekle",
                    tint = TextSecondary)
            }
            Box(
                Modifier.size(48.dp).onGloballyPositioned { handle.value = it }.pointerInput(shortcut.id) {
                    fun pointerInViewport(position: Offset): Float? {
                        val viewport = drag.viewport?.takeIf { it.isAttached } ?: return null
                        val coordinates = handle.value?.takeIf { it.isAttached } ?: return null
                        return viewport.localPositionOf(coordinates, position).y
                    }
                    detectDragGestures(
                        onDragStart = { position -> pointerInViewport(position)?.let { start(it) } },
                        onDragCancel = { drag.id = null },
                        onDragEnd = { drop() },
                        // Absolute coordinates include touch slop and moving graphics layers.
                        // Summing drag deltas loses the initial slop and shifts the drop by a row.
                        onDrag = { change, _ ->
                            change.consume()
                            pointerInViewport(change.position)?.let { move(it) }
                        }
                    )
                }, contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.DragHandle, "${appearance.title} sıralama tutamağı", tint = TextSecondary)
            }
        }
    }
}
