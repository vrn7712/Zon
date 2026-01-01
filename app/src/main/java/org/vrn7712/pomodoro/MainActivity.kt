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

package org.vrn7712.pomodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.vrn7712.pomodoro.ZonApplication
import org.vrn7712.pomodoro.ui.AppScreen
import org.vrn7712.pomodoro.ui.Screen
import org.vrn7712.pomodoro.ui.settingsScreen.viewModel.SettingsViewModel
import org.vrn7712.pomodoro.ui.theme.ZonTheme
import org.vrn7712.pomodoro.utils.toColor

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels(factoryProducer = { SettingsViewModel.Factory })

    private val appContainer by lazy {
        (application as ZonApplication).container
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer.activityTurnScreenOn = {
            setShowWhenLocked(it)
            setTurnScreenOn(it)
        }

        setContent {
            val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()

            val darkTheme = when (settingsState.theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            val seed = settingsState.colorScheme.toColor()

            val isPlus by settingsViewModel.isPlus.collectAsStateWithLifecycle()

            ZonTheme(
                darkTheme = darkTheme,
                seedColor = seed,
                blackTheme = settingsState.blackTheme
            ) {
                val colorScheme = colorScheme
                LaunchedEffect(colorScheme) {
                    settingsViewModel.updateThemeColors(colorScheme)
                }

                var initialScreen by remember { mutableStateOf<Screen?>(null) }

                LaunchedEffect(Unit) {
                    val completed = appContainer.appPreferenceRepository.getBooleanPreference("is_onboarding_completed") ?: false
                    
                    // Check if opened from widget with navigation target
                    val navigateTo = intent?.getStringExtra("navigate_to")
                    initialScreen = when {
                        !completed -> Screen.Onboarding
                        navigateTo == "stats" -> Screen.Stats.Main
                        else -> Screen.Timer
                    }
                }

                if (initialScreen != null) {
                    AppScreen(
                        initialScreen = initialScreen!!,
                        isPlus = isPlus,
                        isAODEnabled = settingsState.aodEnabled,
                        setTimerFrequency = {
                            appContainer.stateRepository.timerFrequency = it
                        }
                    )
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        // Reduce the timer loop frequency when not visible to save battery
        appContainer.stateRepository.timerFrequency = 1f
    }

    override fun onStart() {
        super.onStart()
        // Increase the timer loop frequency again when visible to make the progress smoother
        appContainer.stateRepository.timerFrequency = 60f
    }
}
