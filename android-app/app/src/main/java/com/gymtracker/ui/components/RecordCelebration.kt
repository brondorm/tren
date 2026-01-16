package com.gymtracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * Данные о побитом рекорде
 */
data class RecordInfo(
    val exerciseName: String,
    val old1RM: Double,
    val new1RM: Double
)

/**
 * Празднование побития рекорда с пиксельным Рони Колеманом и падающими блинами
 *
 * @param records Список побитых рекордов
 * @param onDismiss Callback для закрытия анимации
 */
@Composable
fun RecordCelebration(
    records: List<RecordInfo>,
    onDismiss: () -> Unit
) {
    val isGold = records.size >= 2

    // Состояние анимации
    var isVisible by remember { mutableStateOf(true) }

    // Анимация появления
    val appearAnimation = remember { Animatable(0f) }

    // Анимация Рони (покачивание)
    val infiniteTransition = rememberInfiniteTransition(label = "ronnie")
    val ronnieOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ronnie_bounce"
    )

    // Кадр анимации Рони (0-3)
    val ronnieFrame by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ronnie_frame"
    )

    // Падающие блины
    val plates = remember {
        List(if (isGold) 25 else 15) {
            FallingPlate(
                x = Random.nextFloat(),
                delay = Random.nextFloat() * 2000f,
                speed = 0.3f + Random.nextFloat() * 0.4f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 5f,
                size = 30f + Random.nextFloat() * 30f
            )
        }
    }

    // Анимация времени для блинов
    val plateTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "plate_time"
    )

    LaunchedEffect(Unit) {
        appearAnimation.animateTo(
            1f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
    }

    // Автоматическое закрытие через 5 секунд
    LaunchedEffect(Unit) {
        delay(5000)
        isVisible = false
        onDismiss()
    }

    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f * appearAnimation.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isVisible = false
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            // Падающие блины (на заднем плане)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val currentTime = plateTime
                plates.forEach { plate ->
                    val effectiveTime = (currentTime - plate.delay).coerceAtLeast(0f)
                    val y = (effectiveTime * plate.speed) % (size.height + 100f) - 50f
                    val x = plate.x * size.width
                    val rotation = plate.rotation + effectiveTime * plate.rotationSpeed

                    drawPlate(
                        center = Offset(x, y),
                        radius = plate.size,
                        rotation = rotation,
                        isGold = isGold
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                // Заголовок
                Text(
                    text = if (isGold) "🏆 СУПЕР РЕКОРД! 🏆" else "💪 НОВЫЙ РЕКОРД! 💪",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGold) Color(0xFFFFD700) else Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Пиксельный Рони Колеман
                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .offset(y = ronnieOffset.dp)
                ) {
                    drawPixelRonnie(
                        frame = ronnieFrame.toInt() % 4,
                        isGold = isGold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Текст "LIGHTWEIGHT BABY!"
                Text(
                    text = "LIGHTWEIGHT BABY!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGold) Color(0xFFFFD700) else Color(0xFFFF6B6B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Список рекордов
                records.forEach { record ->
                    Text(
                        text = record.exerciseName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${String.format("%.1f", record.old1RM)} → ${String.format("%.1f", record.new1RM)} кг",
                        fontSize = 16.sp,
                        color = Color(0xFF90EE90),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Нажмите для продолжения",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Данные падающего блина
 */
private data class FallingPlate(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val size: Float
)

/**
 * Рисует падающий блин
 */
private fun DrawScope.drawPlate(
    center: Offset,
    radius: Float,
    rotation: Float,
    isGold: Boolean
) {
    val plateColor = if (isGold) Color(0xFFFFD700) else Color(0xFF4A4A4A)
    val rimColor = if (isGold) Color(0xFFDAA520) else Color(0xFF2A2A2A)
    val holeColor = Color(0xFF1A1A1A)

    // Внешний обод
    drawCircle(
        color = rimColor,
        radius = radius,
        center = center
    )

    // Основной блин
    drawCircle(
        color = plateColor,
        radius = radius * 0.85f,
        center = center
    )

    // Отверстие
    drawCircle(
        color = holeColor,
        radius = radius * 0.2f,
        center = center
    )

    // Блик
    drawCircle(
        color = Color.White.copy(alpha = 0.3f),
        radius = radius * 0.15f,
        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f)
    )
}

/**
 * Рисует пиксельного Рони Колемана
 */
private fun DrawScope.drawPixelRonnie(frame: Int, isGold: Boolean) {
    val pixelSize = size.width / 16f

    // Цвета
    val skinColor = Color(0xFF8B6914)
    val skinShadow = Color(0xFF5C4610)
    val muscleHighlight = Color(0xFFA67C1A)
    val shortsColor = if (isGold) Color(0xFFFFD700) else Color(0xFF1E40AF)
    val outlineColor = Color(0xFF1A1A1A)

    // Смещение для анимации поднятия рук
    val armOffset = when (frame) {
        0 -> 0
        1 -> -1
        2 -> -2
        3 -> -1
        else -> 0
    }

    // Пиксельная карта Рони (16x16)
    // Легенда: 0=пусто, 1=кожа, 2=тень, 3=блик, 4=шорты, 5=контур

    // Голова (ряды 0-3)
    val head = listOf(
        listOf(0,0,0,0,0,5,5,5,5,5,5,0,0,0,0,0),
        listOf(0,0,0,0,5,1,1,1,1,1,1,5,0,0,0,0),
        listOf(0,0,0,0,5,1,5,1,1,5,1,5,0,0,0,0), // глаза
        listOf(0,0,0,0,0,5,1,1,1,1,5,0,0,0,0,0)  // рот/подбородок
    )

    // Тело с руками - меняется в зависимости от кадра
    val bodyFrames = listOf(
        // Кадр 0 - руки внизу
        listOf(
            listOf(0,0,0,0,0,0,2,1,1,2,0,0,0,0,0,0), // шея
            listOf(0,0,5,2,2,1,1,1,1,1,1,2,2,5,0,0), // плечи
            listOf(0,5,1,3,1,0,1,1,1,1,0,1,3,1,5,0), // грудь + бицепс
            listOf(0,5,1,1,1,0,1,1,1,1,0,1,1,1,5,0), // грудь
            listOf(0,0,5,2,0,0,2,1,1,2,0,0,2,5,0,0), // талия + предплечье
            listOf(0,0,5,2,0,0,4,4,4,4,0,0,2,5,0,0), // шорты + предплечье
            listOf(0,0,0,5,0,0,4,4,4,4,0,0,5,0,0,0)  // шорты
        ),
        // Кадр 1 - руки поднимаются
        listOf(
            listOf(0,0,0,0,0,0,2,1,1,2,0,0,0,0,0,0),
            listOf(0,5,2,2,2,1,1,1,1,1,1,2,2,2,5,0),
            listOf(5,1,3,1,1,0,1,1,1,1,0,1,1,3,1,5),
            listOf(0,5,1,1,0,0,1,1,1,1,0,0,1,1,5,0),
            listOf(0,0,5,0,0,0,2,1,1,2,0,0,0,5,0,0),
            listOf(0,0,0,0,0,0,4,4,4,4,0,0,0,0,0,0),
            listOf(0,0,0,0,0,0,4,4,4,4,0,0,0,0,0,0)
        ),
        // Кадр 2 - руки вверху (двойной бицепс)
        listOf(
            listOf(0,5,1,5,0,0,2,1,1,2,0,0,5,1,5,0),
            listOf(5,3,1,2,2,1,1,1,1,1,1,2,2,1,3,5),
            listOf(0,5,1,1,1,0,1,1,1,1,0,1,1,1,5,0),
            listOf(0,0,5,2,0,0,1,1,1,1,0,0,2,5,0,0),
            listOf(0,0,0,0,0,0,2,1,1,2,0,0,0,0,0,0),
            listOf(0,0,0,0,0,0,4,4,4,4,0,0,0,0,0,0),
            listOf(0,0,0,0,0,0,4,4,4,4,0,0,0,0,0,0)
        ),
        // Кадр 3 - руки опускаются
        listOf(
            listOf(0,0,0,0,0,0,2,1,1,2,0,0,0,0,0,0),
            listOf(0,5,2,2,2,1,1,1,1,1,1,2,2,2,5,0),
            listOf(5,1,3,1,1,0,1,1,1,1,0,1,1,3,1,5),
            listOf(0,5,1,1,0,0,1,1,1,1,0,0,1,1,5,0),
            listOf(0,0,5,0,0,0,2,1,1,2,0,0,0,5,0,0),
            listOf(0,0,0,0,0,0,4,4,4,4,0,0,0,0,0,0),
            listOf(0,0,0,0,0,0,4,4,4,4,0,0,0,0,0,0)
        )
    )

    // Ноги (статичные)
    val legs = listOf(
        listOf(0,0,0,0,0,0,2,1,1,2,0,0,0,0,0,0), // бёдра
        listOf(0,0,0,0,0,0,1,1,1,1,0,0,0,0,0,0), // колени
        listOf(0,0,0,0,0,0,1,2,2,1,0,0,0,0,0,0), // голени
        listOf(0,0,0,0,0,0,5,5,5,5,0,0,0,0,0,0)  // ступни
    )

    // Собираем полную фигуру
    val fullBody = head + bodyFrames[frame] + legs

    // Рисуем пиксели
    fullBody.forEachIndexed { row, rowData ->
        rowData.forEachIndexed { col, pixel ->
            val color = when (pixel) {
                1 -> skinColor
                2 -> skinShadow
                3 -> muscleHighlight
                4 -> shortsColor
                5 -> outlineColor
                else -> null
            }

            color?.let {
                drawRect(
                    color = it,
                    topLeft = Offset(col * pixelSize, row * pixelSize),
                    size = Size(pixelSize, pixelSize)
                )
            }
        }
    }
}
