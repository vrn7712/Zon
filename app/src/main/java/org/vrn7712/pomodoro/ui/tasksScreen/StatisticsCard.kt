package org.vrn7712.pomodoro.ui.tasksScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.vrn7712.pomodoro.ui.tasksScreen.viewModel.TaskStats
import org.vrn7712.pomodoro.ui.theme.AppFonts

@Composable
fun StatisticsCard(
    stats: TaskStats,
    modifier: Modifier = Modifier
) {
    val progress = if (stats.total > 0) stats.completed.toFloat() / stats.total else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "Progress")

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Daily Progress",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${stats.completed}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = AppFonts.googleFlex600,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "/${stats.total} tasks",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    stats.total == 0 -> "No tasks yet. Add one to get started!"
                    stats.completed == stats.total -> "All done! Great job! \uD83C\uDF89"
                    stats.completed > stats.total / 2 -> "You're doing great! Keep it up."
                    else -> "Let's get to work!"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}
