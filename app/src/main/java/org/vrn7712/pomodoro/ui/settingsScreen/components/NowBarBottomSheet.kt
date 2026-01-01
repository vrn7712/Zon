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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.vrn7712.pomodoro.R
import org.vrn7712.pomodoro.ui.theme.AppFonts.googleFlex600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowBarBottomSheet(
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
                    painterResource(R.drawable.mobile_text),
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Now Bar Instructions",
                    style = typography.headlineSmall,
                    fontFamily = googleFlex600,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "Samsung OneUI 8+ Only",
                style = typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Important note
            Text(
                "⚠️ Important",
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Samsung has disabled Now Bar for external apps by default in OneUI 8. " +
                "It will likely be enabled by default in OneUI 8.5 or later updates.",
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Steps header
            Text(
                "How to Enable Now Bar",
                style = typography.titleMedium,
                fontFamily = googleFlex600
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Step 1: Enable Developer Options
            StepItem(
                stepNumber = 1,
                title = "Enable Developer Options",
                instructions = listOf(
                    "Open Settings app",
                    "Go to About phone",
                    "Tap on Software information",
                    "Tap on Build Number 7 times quickly",
                    "Enter your PIN/password when prompted",
                    "You'll see \"Developer mode has been enabled\""
                )
            )
            
            Spacer(Modifier.height(20.dp))
            
            // Step 2: Enable Live Notifications
            StepItem(
                stepNumber = 2,
                title = "Enable Live Notifications for All Apps",
                instructions = listOf(
                    "Open Settings app",
                    "Scroll down and tap Developer options",
                    "Find \"Live notifications for all apps\"",
                    "Toggle it ON",
                    "Zon's timer will now appear in the Now Bar!"
                )
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Success note
            Text(
                "✅ After enabling, start a focus session and you'll see the timer countdown in your Samsung Now Bar!",
                style = typography.bodyMedium,
                color = colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: Int,
    title: String,
    instructions: List<String>
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step number badge
            Text(
                text = stepNumber.toString(),
                style = typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onPrimary,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary)
                    .padding(6.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        Column(
            modifier = Modifier.padding(start = 40.dp)
        ) {
            instructions.forEachIndexed { index, instruction ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "•",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = instruction,
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
