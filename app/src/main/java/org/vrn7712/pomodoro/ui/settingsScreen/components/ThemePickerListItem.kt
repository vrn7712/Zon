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

package org.vrn7712.pomodoro.ui.settingsScreen.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import org.vrn7712.pomodoro.R
import org.vrn7712.pomodoro.ui.theme.CustomColors.listItemColors
import org.vrn7712.pomodoro.ui.theme.ZonShapeDefaults.bottomListItemShape
import org.vrn7712.pomodoro.ui.theme.ZonShapeDefaults.cardShape
import org.vrn7712.pomodoro.ui.theme.ZonShapeDefaults.middleListItemShape
import org.vrn7712.pomodoro.ui.theme.ZonShapeDefaults.topListItemShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemePickerListItem(
    theme: String,
    items: Int,
    index: Int,
    onThemeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeMap: Map<String, Pair<Int, Int>> = remember {
        mapOf(
            "auto" to Pair(
                R.drawable.brightness_auto,
                R.string.system_default
            ),
            "light" to Pair(R.drawable.light_mode, R.string.light),
            "dark" to Pair(R.drawable.dark_mode, R.string.dark)
        )
    }

    Column(
        modifier
            .clip(
                if (items > 1)
                    when (index) {
                        0 -> topListItemShape
                        items - 1 -> bottomListItemShape
                        else -> middleListItemShape
                    }
                else cardShape,
            ),
    ) {
        ListItem(
            leadingContent = {
                AnimatedContent(themeMap[theme]!!.first) {
                    Icon(
                        painter = painterResource(it),
                        contentDescription = null,
                    )
                }
            },
            headlineContent = { Text(stringResource(R.string.theme)) },
            colors = listItemColors,
        )

        val options = themeMap.toList()
        val selectedIndex = options.indexOf(Pair(theme, themeMap[theme]))

        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            modifier = Modifier
                .background(listItemColors.containerColor)
                .padding(start = 52.dp, end = 16.dp, bottom = 8.dp)
        ) {
            options.fastForEachIndexed { index, theme ->
                val isSelected = selectedIndex == index
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { onThemeChange(theme.first) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                    shapes =
                        when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                ) {
                    Text(
                        stringResource(theme.second.second),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
