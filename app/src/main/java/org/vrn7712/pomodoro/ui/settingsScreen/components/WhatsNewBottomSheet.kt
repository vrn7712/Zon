/*
 * Copyright (c) 2025 Vrushal
 *
 * This file is part of Zon - a minimalist pomodoro timer for Android.
 *
 * Zon is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package org.vrn7712.pomodoro.ui.settingsScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.vrn7712.pomodoro.BuildConfig
import org.vrn7712.pomodoro.R
import org.vrn7712.pomodoro.ui.theme.AppFonts.googleFlex600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painterResource(R.drawable.new_releases),
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "What's New",
                    style = typography.headlineSmall,
                    fontFamily = googleFlex600,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(24.dp))
            
            // New Features Section
            FeatureSection(
                title = "✨ New Features",
                features = listOf(
                    "Custom Timer Sets - Long-press the timer to create and switch between presets (15/5/10, 25/5/15, etc.)",
                    "Manual Time Logging - Tap \"+ Add Manually\" on Stats to log focus time when you couldn't use the timer",
                    "Lifetime Stats - See your total focus time across all recorded days",
                    "Widget Improvements - Better preview images in widget picker",
                    "Focus Sounds - Background music during focus sessions"
                )
            )
            
            Spacer(Modifier.height(20.dp))
            
            // Improvements Section
            FeatureSection(
                title = "🚀 Improvements",
                features = listOf(
                    "Preset Sync - Timer sets are included in backup/restore",
                    "Widget Compatibility - Fixed rounded corners on older Android versions",
                    "Performance - Optimized stats and timer operations",
                    "Material 3 Expressive - Enhanced animations and interactions"
                )
            )
            
            Spacer(Modifier.height(20.dp))
            
            // Bug Fixes Section
            FeatureSection(
                title = "🐛 Bug Fixes",
                features = listOf(
                    "Fixed potential crashes with negative time values",
                    "Improved timer service stability",
                    "Various UI polish and refinements"
                )
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Thank you note
            Text(
                "❤️ Thank you for using Zon! Your feedback helps make the app better.",
                style = typography.bodyMedium,
                color = colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun FeatureSection(
    title: String,
    features: List<String>
) {
    Column {
        Text(
            title,
            style = typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(12.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "•",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        feature,
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
