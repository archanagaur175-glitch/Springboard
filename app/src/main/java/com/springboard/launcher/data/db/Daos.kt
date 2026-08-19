package com.springboard.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GridItemDao {
    @Query("SELECT * FROM grid_items ORDER BY page ASC, slot ASC")
    fun observeAll(): Flow<List<GridItemEntity>>

    @Query("SELECT * FROM grid_items ORDER BY page ASC, slot ASC")
    suspend fun getAll(): List<GridItemEntity>

    @Insert
    suspend fun insert(item: GridItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<GridItemEntity>)

    @Query("DELETE FROM grid_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM grid_items WHERE type = :type AND refKey = :refKey")
    suspend fun deleteByRef(type: String, refKey: String)

    @Query("UPDATE grid_items SET page = :page, slot = :slot WHERE id = :id")
    suspend fun updatePosition(id: Long, page: Int, slot: Int)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY id")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY id")
    suspend fun getAll(): List<FolderEntity>

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)
}

@Dao
interface FolderMemberDao {
    @Query("SELECT * FROM folder_members WHERE folderId = :folderId ORDER BY slot")
    fun observeByFolder(folderId: Long): Flow<List<FolderMemberEntity>>

    @Query("SELECT * FROM folder_members ORDER BY folderId, slot")
    suspend fun all(): List<FolderMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: FolderMemberEntity)

    @Query("DELETE FROM folder_members WHERE folderId = :folderId AND packageName = :packageName")
    suspend fun remove(folderId: Long, packageName: String)

    @Query("UPDATE folder_members SET slot = :slot WHERE folderId = :folderId AND packageName = :packageName")
    suspend fun updateSlot(folderId: Long, packageName: String, slot: Int)

    @Query("SELECT COUNT(*) FROM folder_members WHERE folderId = :folderId")
    suspend fun count(folderId: Long): Int
}

@Dao
interface DockItemDao {
    @Query("SELECT * FROM dock_items ORDER BY slot")
    fun observeAll(): Flow<List<DockItemEntity>>

    @Query("SELECT * FROM dock_items ORDER BY slot")
    suspend fun getAll(): List<DockItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: DockItemEntity)

    @Query("DELETE FROM dock_items WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("UPDATE dock_items SET slot = :slot WHERE packageName = :packageName")
    suspend fun updateSlot(packageName: String, slot: Int)
}