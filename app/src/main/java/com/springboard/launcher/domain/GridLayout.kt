package com.springboard.launcher.domain

import com.springboard.launcher.data.db.GridItemEntity

/**
 * Pure grid math for the fixed-size home screen layout (4 columns x 6 rows), shared
 * by the launcher UI and the layout repository so page/slot assignments agree.
 */
object GridLayout {
    const val COLUMNS = 4
    const val ROWS = 6
    const val PAGE_SIZE = COLUMNS * ROWS

    fun pageCount(itemCount: Int): Int = ((itemCount - 1) / PAGE_SIZE).coerceAtLeast(0) + 1

    fun indexOf(page: Int, slot: Int): Int = page * PAGE_SIZE + slot

    fun pageOf(index: Int): Int = index / PAGE_SIZE

    fun slotOf(index: Int): Int = index % PAGE_SIZE

    fun sortKey(item: GridItemEntity): Int = item.page * PAGE_SIZE + item.slot

    fun withPosition(item: GridItemEntity, index: Int): GridItemEntity =
        item.copy(page = pageOf(index), slot = slotOf(index))

    /** Reflows a list into packed, continuous page/slot positions (no holes). */
    fun compact(items: List<GridItemEntity>): List<GridItemEntity> =
        items.sortedBy { sortKey(it) }.mapIndexed { index, item -> withPosition(item, index) }
}