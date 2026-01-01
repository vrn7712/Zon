/*
 * Copyright (c) 2025 Vrushal
 *
 * This file is part of Zon - a minimalist pomodoro timer for Android.
 */

package org.vrn7712.pomodoro.ui.timerScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CreatePresetDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, focusMinutes: Int, shortBreakMinutes: Int, longBreakMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var focusMinutes by remember { mutableIntStateOf(25) }
    var shortBreakMinutes by remember { mutableIntStateOf(5) }
    var longBreakMinutes by remember { mutableIntStateOf(15) }
    var customName by remember { mutableStateOf("") }
    
    // Auto-generate name if not provided
    val displayName = remember(customName, focusMinutes, shortBreakMinutes, longBreakMinutes) { 
        if (customName.isBlank()) "$focusMinutes/$shortBreakMinutes/$longBreakMinutes"
        else customName
    }
    
    // Validation - always valid since sliders constrain the range
    val isValid = remember(focusMinutes, shortBreakMinutes, longBreakMinutes) {
        focusMinutes in 1..120 && 
        shortBreakMinutes in 1..60 && 
        longBreakMinutes in 1..60
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create New Set",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Optional Name
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Name (optional)") },
                    placeholder = { Text(displayName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Focus Time
                TimeSlider(
                    label = "Focus",
                    value = focusMinutes,
                    onValueChange = { focusMinutes = it },
                    range = 1..120
                )
                
                // Short Break
                TimeSlider(
                    label = "Short Break",
                    value = shortBreakMinutes,
                    onValueChange = { shortBreakMinutes = it },
                    range = 1..60
                )
                
                // Long Break
                TimeSlider(
                    label = "Long Break",
                    value = longBreakMinutes,
                    onValueChange = { longBreakMinutes = it },
                    range = 1..60
                )
                
                // Preview
                Text(
                    text = "Preview: $focusMinutes/$shortBreakMinutes/$longBreakMinutes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    onCreate(displayName, focusMinutes, shortBreakMinutes, longBreakMinutes)
                    onDismiss()
                },
                enabled = isValid
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun TimeSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$value min",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
