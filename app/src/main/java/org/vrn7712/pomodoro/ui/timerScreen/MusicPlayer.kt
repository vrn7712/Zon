/*
 * Copyright (c) 2025 Vrushal
 *
 * This file is part of Zon (forked from Tomato) - a minimalist pomodoro timer for Android.
 *
 * Zon is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Zon is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Zon.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.vrn7712.pomodoro.ui.timerScreen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.vrn7712.pomodoro.R
import kotlin.math.sin

@Composable
fun MusicPlayer(
    isPlaying: Boolean,
    progress: Float,
    currentTime: String,
    totalTime: String,
    isLoading: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Track whether user is dragging
    var isDragging by remember { mutableFloatStateOf(-1f) }
    
    // derivedStateOf: Only recompute when isDragging or progress actually changes
    val displayProgress by remember {
        derivedStateOf { if (isDragging >= 0) isDragging else progress }
    }
    
    // View-based haptics for reliable feedback across devices
    val view = androidx.compose.ui.platform.LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Squiggle Progress Bar with Time Labels
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Custom Squiggle Progress Bar
            SquiggleProgressBar(
                progress = displayProgress.coerceIn(0f, 1f),
                onProgressChange = if (onSeek != null) { newProgress ->
                    isDragging = newProgress
                } else null,
                onProgressChangeFinished = {
                    if (isDragging >= 0 && onSeek != null) {
                        onSeek(isDragging)
                    }
                    isDragging = -1f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )

            // Time Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = totalTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onSkipPrevious()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_next), 
                    contentDescription = "Previous",
                    modifier = Modifier.size(24.dp).rotate(180f)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            IconButton(
                onClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onPlayPause()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.size(64.dp)
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(24.dp))

            IconButton(
                onClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onSkipNext()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_next),
                    contentDescription = "Next",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * A custom squiggle/wiggle progress bar with animated wavy thumb indicator
 */
@Composable
fun SquiggleProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.onSurface,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onProgressChange: ((Float) -> Unit)? = null,
    onProgressChangeFinished: (() -> Unit)? = null
) {
    // Bolder stroke width
    val strokeWidth = 4.dp
    
    // Continuous wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    
    val view = androidx.compose.ui.platform.LocalView.current

    Canvas(
        modifier = modifier
            .pointerInput(onProgressChange) {
                if (onProgressChange != null) {
                    detectTapGestures { offset ->
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onProgressChange(newProgress)
                        onProgressChangeFinished?.invoke()
                    }
                }
            }
            .pointerInput(onProgressChange) {
                if (onProgressChange != null) {
                    detectHorizontalDragGestures(
                        onDragStart = { 
                             view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        onDragEnd = { onProgressChangeFinished?.invoke() },
                        onDragCancel = { onProgressChangeFinished?.invoke() }
                    ) { change, _ ->
                        change.consume()
                        val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        onProgressChange(newProgress)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val strokePx = strokeWidth.toPx()
        
        // Calculate thumb position
        val thumbX = width * progress
        
        // Wave parameters - gentle wave like reference image
        val amplitude = height * 0.15f // Smaller amplitude for subtle wave
        val wavelength = 28.dp.toPx() // Wider spacing between crests/troughs
        
        // Draw the inactive track (from thumb to end) - straight line
        if (thumbX < width) {
            drawLine(
                color = trackColor,
                start = Offset(thumbX, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }
        
        // Draw the animated wave from START (0) to thumb position
        if (thumbX > 0) {
            val wavePath = Path().apply {
                moveTo(0f, centerY)
                
                var x = 0f
                val endX = thumbX
                
                while (x <= endX) {
                    // Calculate y using sine wave with animated phase
                    // normalizedX uses x directly (from 0) for continuous wave
                    val normalizedX = x / wavelength
                    val y = centerY + amplitude * sin((normalizedX * Math.PI.toFloat() * 2) + wavePhase)
                    lineTo(x, y)
                    x += 2f
                }
            }
            
            drawPath(
                path = wavePath,
                color = thumbColor,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        
        // Draw the vertical thumb line
        val thumbLineHeight = height * 0.7f
        drawLine(
            color = thumbColor,
            start = Offset(thumbX, centerY - thumbLineHeight / 2),
            end = Offset(thumbX, centerY + thumbLineHeight / 2),
            strokeWidth = strokePx * 1.5f,
            cap = StrokeCap.Round
        )
    }
}


