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

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.core.text.isDigitsOnly

object MinutesInputTransformation : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        // Allow up to 3 digits for 100+ minute focus sessions
        if (!this.asCharSequence().isDigitsOnly() || this.length > 3) {
            revertAllChanges()
        }
    }
}

//object MinutesOutputTransformation : OutputTransformation {
//    override fun TextFieldBuffer.transformOutput() {
//        when (this.length) {
//            0 -> insert(0, "00")
//            1 -> insert(0, "0")
//        }
//    }
//}
