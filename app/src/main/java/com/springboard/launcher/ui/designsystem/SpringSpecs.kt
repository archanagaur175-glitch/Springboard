package com.springboard.launcher.ui.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Central spring physics specs. All user-visible motion (page paging, jiggle absorb,
 * panel open/close, dock bounce, lock swipe-up) goes through these so the whole system
 * reads as one coherent (springy, iOS-like) motion language. No linear/ease interpolators
 * are used for user-visible motion.
 */
object SpringSpecs {
    /** Snappy, slightly bouncy — page snaps, panel settle. */
    val Snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** Page-to-page paging drag — underdamped but gentle. */
    val Page = spring<Float>(
        dampingRatio = 0.92f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Control Center / sheet open-close. */
    val Panel = spring<Float>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Dock bounce / icon settle. */
    val Bouncy = spring<Float>(
        dampingRatio = 0.55f,
        stiffness = Spring.StiffnessMedium,
    )

    /** Lock screen swipe-up hand-off — crisp, critically damped. */
    val LockSwipe = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumHigh,
    )
}