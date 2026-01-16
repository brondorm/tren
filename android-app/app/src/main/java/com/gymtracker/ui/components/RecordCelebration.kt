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
 * Рисует пиксельного Рони Колемана (32x32 пикселей)
 * Детализированный спрайт с анимацией позы двойного бицепса
 */
private fun DrawScope.drawPixelRonnie(frame: Int, isGold: Boolean) {
    val pixelSize = size.width / 32f

    // Цвета (более реалистичная палитра для темной кожи)
    val skin = Color(0xFF8B4513)        // Основной цвет кожи
    val skinLight = Color(0xFFA0522D)   // Блики на мышцах
    val skinDark = Color(0xFF5D2E0C)    // Тени
    val skinDarker = Color(0xFF3D1E08)  // Глубокие тени
    val outline = Color(0xFF1A1A1A)     // Контур
    val shorts = if (isGold) Color(0xFFFFD700) else Color(0xFF1E40AF)  // Трусы
    val shortsDark = if (isGold) Color(0xFFDAA520) else Color(0xFF152E6B)
    val white = Color(0xFFFFFFFF)       // Глаза, зубы
    val teeth = Color(0xFFFFFFFF)       // Улыбка

    // Легенда цветов:
    // 0=пусто, 1=skin, 2=skinLight, 3=skinDark, 4=skinDarker, 5=outline
    // 6=shorts, 7=shortsDark, 8=white, 9=teeth

    // Кадры анимации - руки поднимаются в позу двойного бицепса
    val sprites = listOf(
        // Кадр 0 - руки полусогнуты внизу
        listOf(
            "00000000000000000000000000000000",
            "00000000000055555555000000000000",
            "00000000005511111111550000000000",
            "00000000055111111111115500000000",
            "00000000051118811881115000000000", // глаза
            "00000000051111111111115000000000",
            "00000000005111199111150000000000", // улыбка
            "00000000000551111155000000000000",
            "00000000000005555500000000000000", // шея
            "00000000000053333500000000000000",
            "00000005555533333335555500000000", // плечи
            "00000053332222222222233350000000",
            "00000531111222222222111135000000",
            "00005311112222332222111113500000", // грудь
            "00053111122223333222211111350000",
            "00531111222233333332221111135000",
            "00531112222333333332222111135000",
            "00053112223333333333222111350000",
            "00005311223333333333221135000000", // пресс
            "00000531122333333332211350000000",
            "00000053112233333322113500000000",
            "00000005667766666677665000000000", // трусы
            "00000005531133333311355000000000",
            "00000053311133333311133500000000", // бёдра
            "00000531111333333331111350000000",
            "00000531111333333331111350000000",
            "00000053111133333311113500000000", // колени
            "00000005311113331111135000000000",
            "00000000531111111111350000000000", // голени
            "00000000053111111113500000000000",
            "00000000005555005555000000000000"  // ступни
        ),
        // Кадр 1 - руки поднимаются
        listOf(
            "00000000000000000000000000000000",
            "00000000000055555555000000000000",
            "00000000005511111111550000000000",
            "00000000055111111111115500000000",
            "00000000051118811881115000000000",
            "00000000051111111111115000000000",
            "00000000005111199111150000000000",
            "00000000000551111155000000000000",
            "00000000000005555500000000000000",
            "00000000000053333500000000000000",
            "00000555553533333353555550000000", // плечи шире
            "00005333222222222222222333500000",
            "00053112222222222222222211350000",
            "00531122222223332222222211135000",
            "05311222222233333322222221113500", // грудь
            "53112222222333333332222222111350",
            "53112222223333333333222222111350",
            "05311222233333333333322211113500",
            "00531122233333333333322111350000",
            "00053112233333333333221135000000",
            "00000531223333333332211350000000",
            "00000056667766667766665000000000", // трусы
            "00000005531133333311355000000000",
            "00000053311133333311133500000000",
            "00000531111333333331111350000000",
            "00000531111333333331111350000000",
            "00000053111133333311113500000000",
            "00000005311113331111135000000000",
            "00000000531111111111350000000000",
            "00000000053111111113500000000000",
            "00000000005555005555000000000000",
            "00000000000000000000000000000000"
        ),
        // Кадр 2 - двойной бицепс (основная поза)
        listOf(
            "00000000000000000000000000000000",
            "00005550000055555555000005550000", // кулаки вверху
            "00053335000511111111500533350000",
            "00053335055111111111155033350000",
            "00005350051118811881115005350000", // глаза
            "00053350051111111111115053350000",
            "00531150005111199111150051135000", // улыбка + бицепсы
            "05322115000551111155000511223500",
            "53322211500005555500051122223350", // шея + руки
            "53222211550053333500551122222350",
            "53222221153533333353511222222350", // плечи + бицепс пик
            "05322221222222222222222122223500",
            "00532221222222222222222122235000",
            "00053221222223332222221223500000",
            "00005322222233333322222235000000", // грудь
            "00000532222333333332222350000000",
            "00000053222333333332225300000000",
            "00000005322333333332235000000000",
            "00000000532333333332350000000000", // пресс
            "00000000053233333323500000000000",
            "00000000005323333235000000000000",
            "00000000056677666776650000000000", // трусы
            "00000000053113333115300000000000",
            "00000000531113333111350000000000",
            "00000005311133333311135000000000", // бёдра
            "00000053111333333331113500000000",
            "00000053111333333331113500000000",
            "00000005311133333311135000000000",
            "00000000531113331111350000000000", // голени
            "00000000053111111113500000000000",
            "00000000005555005555000000000000",
            "00000000000000000000000000000000"
        ),
        // Кадр 3 - напряжение бицепса (пик)
        listOf(
            "00000000000000000000000000000000",
            "00053550000055555555000005535000", // кулаки
            "00532235000511111111500532235000",
            "00532235055111111111155032235000",
            "00053350051118811881115005335000",
            "00532250051111111111115052235000",
            "05322215005111199111150512223500", // бицепс пик выше
            "53222211500551111155000511222350",
            "53322221155005555500551122223350",
            "53222222115053333350511222222350",
            "53222222153533333353512222222350", // максимальный пик
            "05322222122222222222221222223500",
            "00532222122222222222221222235000",
            "00053222122223332222221222350000",
            "00005322222233333322222223500000",
            "00000532222333333332222235000000",
            "00000053222333333332222350000000",
            "00000005322333333332223500000000",
            "00000000532333333332350000000000",
            "00000000053233333323500000000000",
            "00000000005323333235000000000000",
            "00000000056677666776650000000000",
            "00000000053113333115300000000000",
            "00000000531113333111350000000000",
            "00000005311133333311135000000000",
            "00000053111333333331113500000000",
            "00000053111333333331113500000000",
            "00000005311133333311135000000000",
            "00000000531113331111350000000000",
            "00000000053111111113500000000000",
            "00000000005555005555000000000000",
            "00000000000000000000000000000000"
        )
    )

    val currentSprite = sprites[frame]

    // Рисуем пиксели
    currentSprite.forEachIndexed { row, rowStr ->
        rowStr.forEachIndexed { col, pixel ->
            val color = when (pixel) {
                '1' -> skin
                '2' -> skinLight
                '3' -> skinDark
                '4' -> skinDarker
                '5' -> outline
                '6' -> shorts
                '7' -> shortsDark
                '8' -> white
                '9' -> teeth
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
