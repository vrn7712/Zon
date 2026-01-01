package org.vrn7712.pomodoro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var rotation: Float,
    var rotationSpeed: Float,
    var size: Float
)

@Composable
fun Confetti(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFFFF5252),
        Color(0xFFFFD740),
        Color(0xFF448AFF),
        Color(0xFF69F0AE),
        Color(0xFFE040FB)
    ),
    isExploding: Boolean = false,
    durationMillis: Long = 3000
) {
    if (!isExploding) return

    var particles by remember {
        mutableStateOf(List(50) { id ->
            Particle(
                id = id,
                x = 0f, // Center horizontally (set in draw)
                y = 0f, // Center vertically
                vx = (Random.nextFloat() - 0.5f) * 30f,
                vy = (Random.nextFloat() - 0.5f) * 30f - 10f, // Upward bias
                color = colors.random(),
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 10f,
                size = Random.nextFloat() * 10f + 5f
            )
        })
    }

    var elapsedTime by remember { mutableLongStateOf(0L) }
    var width by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableFloatStateOf(0f) }
    var initialised by remember { mutableStateOf(false) }

    LaunchedEffect(isExploding) {
        elapsedTime = 0
        initialised = false
        val startTime = System.currentTimeMillis()
        while (elapsedTime < durationMillis) {
            val currentTime = System.currentTimeMillis()
            elapsedTime = currentTime - startTime
            
            // Update particles
            particles = particles.map { p ->
                p.copy(
                    x = p.x + p.vx,
                    y = p.y + p.vy,
                    vy = p.vy + 0.5f, // Gravity
                    rotation = p.rotation + p.rotationSpeed
                )
            }
            delay(16) // roughly 60fps
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        width = size.width
        height = size.height

        if (!initialised) {
            // Respawn particles at center
            particles = particles.map { p ->
                p.copy(
                    x = width / 2f,
                    y = height / 2f,
                    vx = (Random.nextFloat() - 0.5f) * 20f, // Burst spread
                    vy = (Random.nextFloat() * -30f) - 10f // Burst up
                )
            }
            initialised = true
        }

        particles.forEach { p ->
            withTransform({
                rotate(p.rotation, pivot = Offset(p.x, p.y))
                translate(left = p.x, top = p.y)
            }) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(-p.size / 2, -p.size / 2),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size)
                )
            }
        }
    }
}
