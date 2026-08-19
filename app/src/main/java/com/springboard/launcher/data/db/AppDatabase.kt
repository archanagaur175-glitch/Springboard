package com.springboard.launcher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        GridItemEntity::class,
        FolderEntity::class,
        FolderMemberEntity::class,
        DockItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gridItemDao(): GridItemDao
    abstract fun folderDao(): FolderDao
    abstract fun folderMemberDao(): FolderMemberDao
    abstract fun dockItemDao(): DockItemDao

    companion object {
        const val NAME = "springboard.db"
    }
}