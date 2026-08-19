package com.springboard.launcher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.springboard.launcher.data.apps.AppRepository
import com.springboard.launcher.data.apps.InstalledApp
import com.springboard.launcher.data.db.FolderMemberEntity
import com.springboard.launcher.data.system.SystemStateRepository

/** Placeholder; the full folder sheet (glass, reorder, drag-out) lands with jiggle/folder support. */
@Composable
fun FolderSheet(
    folderId: Long,
    folderName: String,
    members: List<FolderMemberEntity>,
    installed: Map<String, InstalledApp>,
    appRepository: AppRepository,
    systemState: SystemStateRepository,
    jiggle: Boolean,
    onTapApp: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(folderName, color = Color.White)
    }
}