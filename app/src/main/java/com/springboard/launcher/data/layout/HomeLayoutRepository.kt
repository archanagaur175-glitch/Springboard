package com.springboard.launcher.data.layout

import com.springboard.launcher.data.db.DockItemDao
import com.springboard.launcher.data.db.DockItemEntity
import com.springboard.launcher.data.db.FolderDao
import com.springboard.launcher.data.db.FolderEntity
import com.springboard.launcher.data.db.FolderMemberDao
import com.springboard.launcher.data.db.FolderMemberEntity
import com.springboard.launcher.data.db.GridItemDao
import com.springboard.launcher.data.db.GridItemEntity
import com.springboard.launcher.data.db.GridItemType
import com.springboard.launcher.domain.GridLayout
import kotlinx.coroutines.flow.Flow

/**
 * Owns the home-screen layout: grid pages, dock pins and folder membership.
 * All mutations are persisted to Room and exposed reactively.
 */
class HomeLayoutRepository(
    private val gridDao: GridItemDao,
    private val folderDao: FolderDao,
    private val memberDao: FolderMemberDao,
    private val dockDao: DockItemDao,
) {
    val grid: Flow<List<GridItemEntity>> = gridDao.observeAll()

    val folders: Flow<List<FolderEntity>> = folderDao.observeAll()

    val dock: Flow<List<DockItemEntity>> = dockDao.observeAll()

    fun folderMembers(folderId: Long): Flow<List<FolderMemberEntity>> = memberDao.observeByFolder(folderId)

    /** Adds newly installed apps to the end of the grid and removes uninstalled ones everywhere. */
    suspend fun syncInstalled(apps: List<String>) {
        val installed = apps.toSet()
        val current = gridDao.getAll()
        val currentByRef = current.filter { it.isApp }.associateBy { it.refKey }

        val staleApps = current.filter { it.isApp && it.refKey !in installed }
        staleApps.forEach { gridDao.deleteById(it.id) }

        val staleDock = dockDao.getAll().filter { it.packageName !in installed }
        staleDock.forEach {
            dockDao.delete(it.packageName)
            reslotDock()
        }

        val staleMembers = memberDao.all().filter { it.packageName !in installed }
        staleMembers.forEach { memberDao.remove(it.folderId, it.packageName) }
        emptyFolders().forEach { folderDao.delete(it) }

        val newApps = apps.sorted().filterNot { it in currentByRef }
        if (newApps.isNotEmpty()) {
            val compacted = compactGridItems(current)
            var nextIndex = compacted.size
            val insert = newApps.map { pkg ->
                val index = nextIndex++
                GridItemEntity(
                    type = GridItemType.APP,
                    refKey = pkg,
                    page = GridLayout.pageOf(index),
                    slot = GridLayout.slotOf(index),
                )
            }
            gridDao.insertAll(insert)
        }
        compactGrid()
    }

    /** Reassigns continuous page/slot positions so removed apps don't leave holes. */
    suspend fun compactGrid() {
        val items = gridDao.getAll()
        compactGridItems(items).forEach { updated ->
            gridDao.updatePosition(updated.id, updated.page, updated.slot)
        }
    }

    private fun compactGridItems(items: List<GridItemEntity>): List<GridItemEntity> =
        GridLayout.compact(items)

    suspend fun removeFromGrid(packageName: String) {
        gridDao.deleteByRef(GridItemType.APP, packageName)
        compactGrid()
    }

    suspend fun addToGrid(packageName: String) {
        val items = gridDao.getAll()
        val index = (items.maxOfOrNull { GridLayout.sortKey(it) } ?: -1) + 1
        gridDao.insert(
            GridItemEntity(
                type = GridItemType.APP,
                refKey = packageName,
                page = GridLayout.pageOf(index),
                slot = GridLayout.slotOf(index),
            ),
        )
    }

    /** Swaps the layout positions of two grid items (used during jiggle reorder). */
    suspend fun swapGridPositions(a: GridItemEntity, b: GridItemEntity) {
        gridDao.updatePosition(a.id, b.page, b.slot)
        gridDao.updatePosition(b.id, a.page, a.slot)
    }

    suspend fun moveGridItemTo(item: GridItemEntity, targetIndex: Int) {
        val tPage = GridLayout.pageOf(targetIndex)
        val tSlot = GridLayout.slotOf(targetIndex)
        val existing = gridDao.getAll().firstOrNull { it.page == tPage && it.slot == tSlot && it.id != item.id }
        if (existing != null) {
            swapGridPositions(item, existing)
        } else {
            gridDao.updatePosition(item.id, tPage, tSlot)
        }
        compactGrid()
    }

    // ==================== Folders ====================

    /** Creates a folder containing a and b; both are removed from the grid. */
    suspend fun createFolder(a: String, b: String): Long {
        val folderId = folderDao.insert(FolderEntity(name = "Folder"))
        memberDao.insert(FolderMemberEntity(folderId, a, 0))
        memberDao.insert(FolderMemberEntity(folderId, b, 1))
        gridDao.deleteByRef(GridItemType.APP, a)
        gridDao.deleteByRef(GridItemType.APP, b)
        val items = gridDao.getAll()
        val index = (items.maxOfOrNull { GridLayout.sortKey(it) } ?: -1) + 1
        gridDao.insert(
            GridItemEntity(
                type = GridItemType.FOLDER,
                refKey = folderId.toString(),
                page = GridLayout.pageOf(index),
                slot = GridLayout.slotOf(index),
            ),
        )
        compactGrid()
        return folderId
    }

    suspend fun addToFolder(folderId: Long, packageName: String) {
        val count = memberDao.count(folderId)
        memberDao.insert(FolderMemberEntity(folderId, packageName, count))
        gridDao.deleteByRef(GridItemType.APP, packageName)
        compactGrid()
    }

    /** Removes a member; moves it back onto the grid unless [addToGrid] is false. */
    suspend fun removeFromFolder(folderId: Long, packageName: String, addToGrid: Boolean = true) {
        memberDao.remove(folderId, packageName)
        if (addToGrid) this.addToGrid(packageName)
        if (memberDao.count(folderId) == 0) {
            folderDao.delete(folderId)
            gridDao.deleteByRef(GridItemType.FOLDER, folderId.toString())
            compactGrid()
        }
    }

    suspend fun renameFolder(folderId: Long, name: String) {
        folderDao.rename(folderId, name)
    }

    suspend fun removeFolder(folderId: Long) {
        memberDao.clearForFolder(folderId)
        folderDao.delete(folderId)
        gridDao.deleteByRef(GridItemType.FOLDER, folderId.toString())
        compactGrid()
    }

    suspend fun moveMember(folderId: Long, packageName: String, slot: Int) {
        memberDao.updateSlot(folderId, packageName, slot)
    }

    private suspend fun emptyFolders(): List<Long> {
        val members = memberDao.all().map { it.folderId }.toSet()
        return folderDao.getAll().map { it.id }.filterNot { it in members }
    }

    // ==================== Dock ====================

    suspend fun addToDock(packageName: String) {
        val slot = (dockDao.getAll().maxOfOrNull { it.slot } ?: -1) + 1
        dockDao.insert(DockItemEntity(packageName, slot))
    }

    suspend fun removeFromDock(packageName: String) {
        dockDao.delete(packageName)
        reslotDock()
    }

    suspend fun isInDock(packageName: String): Boolean = dockDao.getAll().any { it.packageName == packageName }

    suspend fun swapDock(a: String, b: String) {
        val items = dockDao.getAll()
        val ai = items.firstOrNull { it.packageName == a } ?: return
        val bi = items.firstOrNull { it.packageName == b } ?: return
        dockDao.updateSlot(a, bi.slot)
        dockDao.updateSlot(b, ai.slot)
    }

    private suspend fun reslotDock() {
        dockDao.getAll()
            .sortedBy { it.slot }
            .forEachIndexed { index, item -> dockDao.updateSlot(item.packageName, index) }
    }
}