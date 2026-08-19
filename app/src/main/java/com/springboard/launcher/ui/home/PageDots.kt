package com.springboard.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PageDots(
    count: Int,
    current: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { index ->
            val selected = index == current
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = if (selected) 18.dp else 7.dp, height = 7.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else Color.White.copy(alpha = 0.5f))
                    .clickable { onSelect(index) },
            )
        }
    }
}