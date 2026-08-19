package com.springboard.launcher.domain

import com.springboard.launcher.data.db.GridItemEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class GridLayoutTest {

    @Test
    fun `page count covers empty, one, full and overflow pages`() {
        assertEquals(1, GridLayout.pageCount(0))
        assertEquals(1, GridLayout.pageCount(1))
        assertEquals(1, GridLayout.pageCount(GridLayout.PAGE_SIZE))
        assertEquals(2, GridLayout.pageCount(GridLayout.PAGE_SIZE + 1))
    }

    @Test
    fun `index and page slot round trip`() {
        for (index in listOf(0, 1, 23, 24, 47, 48, 100)) {
            assertEquals(index, GridLayout.indexOf(GridLayout.pageOf(index), GridLayout.slotOf(index)))
        }
        assertEquals(1, GridLayout.pageOf(24))
        assertEquals(0, GridLayout.slotOf(24))
    }

    @Test
    fun `sort key orders by page then slot`() {
        val a = GridItemEntity(type = "APP", refKey = "a", page = 0, slot = 5)
        val b = GridItemEntity(type = "APP", refKey = "b", page = 1, slot = 0)
        assertEquals(5, GridLayout.sortKey(a))
        assertEquals(GridLayout.PAGE_SIZE, GridLayout.sortKey(b))
        assertLess(GridLayout.sortKey(a), GridLayout.sortKey(b))
    }

    @Test
    fun `compact fills holes in order`() {
        val items = listOf(
            GridItemEntity(id = 1, type = "APP", refKey = "first", page = 0, slot = 0),
            GridItemEntity(id = 2, type = "APP", refKey = "hole", page = 0, slot = 3),
            GridItemEntity(id = 3, type = "FOLDER", refKey = "9", page = 1, slot = 0),
        )
        val compacted = GridLayout.compact(items)
        assertEquals(0, compacted[0].slot)
        assertEquals(0, compacted[0].page)
        assertEquals(1, compacted[1].slot)
        assertEquals(2, compacted[2].slot)
        assertEquals(0, compacted[2].page)
    }

    @Test
    fun `withPosition rebases onto a new absolute index`() {
        val item = GridItemEntity(type = "APP", refKey = "x", page = 0, slot = 0)
        val moved = GridLayout.withPosition(item, GridLayout.PAGE_SIZE + 3)
        assertEquals(1, moved.page)
        assertEquals(3, moved.slot)
    }

    private fun assertLess(a: Int, b: Int) {
        if (a >= b) throw AssertionError("expected $a < $b")
    }
}