package com.springboard.launcher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

object GridItemType {
    const val APP = "APP"
    const val FOLDER = "FOLDER"
}

@Entity(tableName = "grid_items")
data class GridItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val refKey: String,
    val page: Int,
    val slot: Int,
) {
    val isApp: Boolean get() = type == GridItemType.APP
    val isFolder: Boolean get() = type == GridItemType.FOLDER
}

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(tableName = "folder_members", primaryKeys = ["folderId", "packageName"])
data class FolderMemberEntity(
    val folderId: Long,
    val packageName: String,
    val slot: Int,
)

@Entity(tableName = "dock_items")
data class DockItemEntity(
    @PrimaryKey val packageName: String,
    val slot: Int,
)