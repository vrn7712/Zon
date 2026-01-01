/*
 * Copyright (c) 2025 Vrushal
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.vrn7712.pomodoro.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun localDateToString(localDate: LocalDate?): String? {
        return localDate?.toString()
    }

    @TypeConverter
    fun stringToLocalDate(date: String?): LocalDate? {
        return if (date != null) LocalDate.parse(date) else null
    }
}
