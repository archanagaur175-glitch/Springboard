package com.springboard.launcher.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.springboard.launcher.data.db.GridItemType

/**
 * The result of a drop hit-test: either the [key] of the hovered item (with its
 * [isFolder] flag) or an empty cell identified by its absolute grid [slotIndex].
 */
data class DragHit(val key: String?, val isFolder: Boolean, val slotIndex: Int?) {
    companion object {
        val Empty = DragHit(null, false, null)
    }
}

/**
 * Shared state for jiggle drag-and-drop on the home screen. A single instance is
 * hoisted by [LauncherScreen] so grid pages and the dock can agree on bounds,
 * the hovered target and the floating ghost position. Mutations trigger Compose
 * recomposition of every subscriber, which is what drives the visual feedback.
 */
class DragController {
    var draggingItemId by mutableStateOf<Long?>(null)
        private set
    var draggingKey by mutableStateOf<String?>(null)
        private set
    var draggingType by mutableStateOf<String?>(null)
        private set
    var dragStartWindow by mutableStateOf(Offset.Zero)
        private set
    var ghostOffset by mutableStateOf(Offset.Zero)
        private set
    var pointerWindow by mutableStateOf(Offset.Zero)
        private set
    var hoveringKey by mutableStateOf<String?>(null)
        private set
    var hoveringIsFolder by mutableStateOf(false)
        private set
    var hoveredEmptySlotIndex by mutableStateOf<Int?>(null)
        private set

    val isDragging: Boolean get() = draggingKey != null

    private val appBounds = mutableMapOf<String, Rect>()
    private val folderBounds = mutableMapOf<String, Rect>()
    private val emptyBounds = mutableMapOf<Int, Rect>()
    private var dockBounds: Rect? = null

    fun registerAppBound(key: String, rect: Rect) {
        appBounds[key] = rect
    }

    fun registerFolderBound(key: String, rect: Rect) {
        folderBounds[key] = rect
    }

    fun registerEmptySlot(slotIndex: Int, rect: Rect) {
        emptyBounds[slotIndex] = rect
    }

    fun registerDock(rect: Rect?) {
        dockBounds = rect
    }

    fun clear() {
        appBounds.clear()
        folderBounds.clear()
        emptyBounds.clear()
        dockBounds = null
    }

    fun beginDrag(itemId: Long, key: String, type: String, pointerWindow: Offset) {
        draggingItemId = itemId
        draggingKey = key
        draggingType = type
        dragStartWindow = pointerWindow
        ghostOffset = Offset.Zero
        pointerWindow = pointerWindow
        updateHover()
    }

    fun move(delta: Offset, pointerWindow: Offset) {
        if (draggingKey == null) return
        ghostOffset += delta
        this.pointerWindow = pointerWindow
        updateHover()
    }

    /** Ends the drag and returns the drop target the pointer was over. */
    fun endDrag(): DragHit {
        val hit = currentHit()
        reset()
        return hit
    }

    fun cancelDrag() {
        reset()
    }

    fun goingToDock(): Boolean = dockBounds?.contains(pointerWindow) == true

    private fun reset() {
        draggingItemId = null
        draggingKey = null
        draggingType = null
        dragStartWindow = Offset.Zero
        ghostOffset = Offset.Zero
        pointerWindow = Offset.Zero
        hoveringKey = null
        hoveringIsFolder = false
        hoveredEmptySlotIndex = null
    }

    private fun currentHit(): DragHit {
        if (draggingKey == null) return DragHit.Empty
        val pos = pointerWindow
        val key = draggingKey
        for ((k, r) in appBounds) {
            if (k != key && r.contains(pos)) return DragHit(k, false, null)
        }
        for ((k, r) in folderBounds) {
            if (r.contains(pos)) return DragHit(k, true, null)
        }
        for ((s, r) in emptyBounds) {
            if (r.contains(pos)) return DragHit(null, false, s)
        }
        return DragHit.Empty
    }

    private fun updateHover() {
        val hit = currentHit()
        hoveringKey = hit.key
        hoveringIsFolder = hit.isFolder
        hoveredEmptySlotIndex = hit.slotIndex
    }
}