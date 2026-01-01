/*
 * Copyright (c) 2025 Vrushal
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.vrn7712.pomodoro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.format.DateTimeFormatter

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE)
    }
}

@Entity(tableName = "stat")
@Serializable
data class Stat(
    @PrimaryKey
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val focusTimeQ1: Long,
    val focusTimeQ2: Long,
    val focusTimeQ3: Long,
    val focusTimeQ4: Long,
    val breakTime: Long
) {
    fun totalFocusTime() = focusTimeQ1 + focusTimeQ2 + focusTimeQ3 + focusTimeQ4
}

data class StatSummary(
    val date: LocalDate,
    val focusTime: Long,
    val breakTime: Long
)

data class StatFocusTime(
    val focusTimeQ1: Long,
    val focusTimeQ2: Long,
    val focusTimeQ3: Long,
    val focusTimeQ4: Long
) {
    fun total() = focusTimeQ1 + focusTimeQ2 + focusTimeQ3 + focusTimeQ4
}

data class StatTime(
    val focusTimeQ1: Long,
    val focusTimeQ2: Long,
    val focusTimeQ3: Long,
    val focusTimeQ4: Long,
    val breakTime: Long,
)
