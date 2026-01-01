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

package org.vrn7712.pomodoro.ui.settingsScreen.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.vrn7712.pomodoro.R
import org.vrn7712.pomodoro.ui.mergePaddingValues
import org.vrn7712.pomodoro.ui.settingsScreen.viewModel.SettingsAction
import org.vrn7712.pomodoro.ui.settingsScreen.viewModel.SettingsState
import org.vrn7712.pomodoro.ui.theme.AppFonts.robotoFlexTopBar
import org.vrn7712.pomodoro.ui.theme.CustomColors.listItemColors
import org.vrn7712.pomodoro.ui.theme.CustomColors.switchColors
import org.vrn7712.pomodoro.ui.theme.CustomColors.topBarColors
import org.vrn7712.pomodoro.ui.theme.ZonShapeDefaults.bottomListItemShape
import org.vrn7712.pomodoro.ui.theme.ZonShapeDefaults.cardShape
import org.vrn7712.pomodoro.ui.theme.ZonShapeDefaults.middleListItemShape
import org.vrn7712.pomodoro.ui.theme.ZonShapeDefaults.topListItemShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MusicSettings(
    settingsState: SettingsState,
    contentPadding: PaddingValues,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current

    // Use OpenDocument instead of GetContent for persistent URI permissions
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                // Take persistent permission so the URI works after app restart
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                onAction(SettingsAction.UpdateMusicSound(it.toString()))
            } catch (e: SecurityException) {
                // If persistent permission fails, still try to use the URI
                // It will work for this session at least
                onAction(SettingsAction.UpdateMusicSound(it.toString()))
            }
        }
    }

    val customSoundName = remember(settingsState.musicSoundUri) {
        if (settingsState.musicSoundUri.isNullOrEmpty()) null
        else {
            try {
                val uri = android.net.Uri.parse(settingsState.musicSoundUri)
                var name = "Custom Audio"
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) name = it.getString(index)
                    }
                }
                name
            } catch (e: Exception) {
                "Custom Audio"
            }
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(stringResource(R.string.focus_sounds), fontFamily = robotoFlexTopBar)
                },
                subtitle = {
                    Text(stringResource(R.string.settings))
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = listItemColors.containerColor)
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            stringResource(R.string.back)
                        )
                    }
                },
                colors = topBarColors,
                scrollBehavior = scrollBehavior
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        val insets = mergePaddingValues(innerPadding, contentPadding)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = insets,
            modifier = Modifier
                .background(topBarColors.containerColor)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(Modifier.height(14.dp)) }

            // Enable/Disable Switch
            item {
                ListItem(
                    leadingContent = {
                        Icon(painterResource(R.drawable.music_note), null)
                    },
                    headlineContent = { Text(stringResource(R.string.focus_sounds)) },
                    supportingContent = { Text("Play ambient sounds during focus sessions") },
                    trailingContent = {
                        Switch(
                            checked = settingsState.isMusicEnabled,
                            onCheckedChange = { onAction(SettingsAction.ToggleMusic(it)) },
                            thumbContent = {
                                if (settingsState.isMusicEnabled) {
                                    Icon(
                                        painter = painterResource(R.drawable.check),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.clear),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                }
                            },
                            colors = switchColors
                        )
                    },
                    colors = listItemColors,
                    modifier = Modifier.clip(cardShape)
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            // Default Track Selection Header
            item {
                Text(
                    text = stringResource(R.string.default_track),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
            }

            // Cozy Lofi Track
            item {
                ListItem(
                    leadingContent = {
                        RadioButton(
                            selected = settingsState.defaultMusicTrack == "cozy_lofi" && settingsState.musicSoundUri.isNullOrEmpty(),
                            onClick = {
                                onAction(SettingsAction.ClearMusicSound)
                                onAction(SettingsAction.UpdateDefaultMusicTrack("cozy_lofi"))
                            }
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.cozy_lofi)) },
                    supportingContent = { Text("Relaxing lofi beats") },
                    colors = listItemColors,
                    modifier = Modifier
                        .clip(topListItemShape)
                        .clickable {
                            onAction(SettingsAction.ClearMusicSound)
                            onAction(SettingsAction.UpdateDefaultMusicTrack("cozy_lofi"))
                        }
                )
            }

            // Study Music Track
            item {
                ListItem(
                    leadingContent = {
                        RadioButton(
                            selected = settingsState.defaultMusicTrack == "study_music" && settingsState.musicSoundUri.isNullOrEmpty(),
                            onClick = {
                                onAction(SettingsAction.ClearMusicSound)
                                onAction(SettingsAction.UpdateDefaultMusicTrack("study_music"))
                            }
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.study_music)) },
                    supportingContent = { Text("Calm music for studying") },
                    colors = listItemColors,
                    modifier = Modifier
                        .clip(bottomListItemShape)
                        .clickable {
                            onAction(SettingsAction.ClearMusicSound)
                            onAction(SettingsAction.UpdateDefaultMusicTrack("study_music"))
                        }
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            // Custom Audio Section Header
            item {
                Text(
                    text = stringResource(R.string.custom_audio),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
            }

            // Custom Audio Item
            item {
                ListItem(
                    leadingContent = {
                        RadioButton(
                            selected = !settingsState.musicSoundUri.isNullOrEmpty(),
                            onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) }
                        )
                    },
                    headlineContent = { 
                        Text(customSoundName ?: stringResource(R.string.select_custom_audio)) 
                    },
                    supportingContent = { 
                        if (customSoundName != null) {
                            Text("Using custom audio file")
                        } else {
                            Text("Choose your own audio file")
                        }
                    },
                    trailingContent = {
                        if (!settingsState.musicSoundUri.isNullOrEmpty()) {
                            IconButton(onClick = { onAction(SettingsAction.ClearMusicSound) }) {
                                Icon(painterResource(R.drawable.close), stringResource(R.string.clear_custom_audio))
                            }
                        } else {
                            Icon(painterResource(R.drawable.arrow_forward_big), null)
                        }
                    },
                    colors = listItemColors,
                    modifier = Modifier
                        .clip(cardShape)
                        .clickable { audioPickerLauncher.launch(arrayOf("audio/*")) }
                )
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
