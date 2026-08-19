package com.springboard.launcher.ui.home

import androidx.compose.runtime.mutableStateOf

/** Interaction hooks for jiggle drag gestures; the real implementation lives with commit 5. */
interface DragHooks {
    val isDragging: Boolean
    fun onDragStart(itemId: Long, position: androidx.compose.ui.geometry.Offset)
    fun onDrag(delta: androidx.compose.ui.geometry.Offset)
    fun onDragEnd()
    fun onDragCancel()
}

object NoopDragHooks : DragHooks {
    override val isDragging: Boolean = false
    override fun onDragStart(itemId: Long, position: androidx.compose.ui.geometry.Offset) {}
    override fun onDrag(delta: androidx.compose.ui.geometry.Offset) {}
    override fun onDragEnd() {}
    override fun onDragCancel() {}
}

/** Shared jiggle/drag state for the launcher. */
class DragController : DragHooks {
    var draggingId by mutableStateOf<Long?>(null)
        private set
    var position by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
        private set
    var hoveringId by mutableStateOf<Long?>(null)

    override val isDragging: Boolean get() = draggingId != null

    override fun onDragStart(itemId: Long, position: androidx.compose.ui.geometry.Offset) {
        draggingId = itemId
        this.position = position
    }

    override fun onDrag(delta: androidx.compose.ui.geometry.Offset) {
        position += delta
    }

    override fun onDragEnd() {
        draggingId = null
        position = androidx.compose.ui.geometry.Offset.Zero
    }

    override fun onDragCancel() {
        draggingId = null
        position = androidx.compose.ui.geometry.Offset.Zero
    }
}