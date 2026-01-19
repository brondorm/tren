package com.gymtracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymtracker.data.database.WeightProgress
import com.gymtracker.data.model.Exercise
import com.gymtracker.data.repository.GymRepository
import com.gymtracker.data.repository.PopularExerciseWithProgress
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    repository: GymRepository,
    onNavigateBack: () -> Unit
) {
    val exercises by repository.allExercises.collectAsState(initial = emptyList())
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var progress by remember { mutableStateOf<List<WeightProgress>>(emptyList()) }
    var popularExercises by remember { mutableStateOf<List<PopularExerciseWithProgress>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Загружаем популярные упражнения при входе
    LaunchedEffect(Unit) {
        isLoading = true
        popularExercises = repository.getTopPopularExercisesWithProgress(5)
        isLoading = false
    }

    // Загружаем прогресс при выборе упражнения
    LaunchedEffect(selectedExercise) {
        selectedExercise?.let { exercise ->
            isLoading = true
            progress = repository.getWeightProgress(exercise.id)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Прогресс") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Блоки популярных упражнений
            if (selectedExercise == null) {
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (popularExercises.isEmpty()) {
                    item {
                        EmptyStateMessage(
                            icon = Icons.Default.FitnessCenter,
                            message = "Начните тренироваться, чтобы видеть прогресс"
                        )
                    }
                } else {
                    item {
                        Text(
                            "Топ упражнений",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    items(popularExercises) { exerciseData ->
                        PopularExerciseCard(
                            exerciseData = exerciseData,
                            onClick = {
                                // Находим упражнение и выбираем его
                                val exercise = exercises.find { it.id == exerciseData.exerciseId }
                                selectedExercise = exercise
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Выбор упражнения
            item {
                Text(
                    if (selectedExercise == null) "Или выберите упражнение" else "Упражнение",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedExercise?.name ?: "Не выбрано",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Row {
                                if (selectedExercise != null) {
                                    IconButton(onClick = {
                                        selectedExercise = null
                                        progress = emptyList()
                                    }) {
                                        Icon(Icons.Default.Close, "Сбросить")
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        exercises.forEach { exercise ->
                            DropdownMenuItem(
                                text = { Text(exercise.name) },
                                onClick = {
                                    selectedExercise = exercise
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Детали выбранного упражнения
            if (selectedExercise != null) {
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (progress.isEmpty()) {
                    item {
                        EmptyStateMessage(
                            icon = Icons.Default.ShowChart,
                            message = "Нет данных для этого упражнения"
                        )
                    }
                } else {
                    // Статистика по расчётному 1RM
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedStatsCard(progress = progress)
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Динамика 1RM",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // График
                        WeightProgressChart(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        )
                    }

                    // Таблица данных
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "История лучших подходов",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ProgressHistoryTable(progress = progress)
                    }
                }
            }
        }
    }
}

@Composable
fun PopularExerciseCard(
    exerciseData: PopularExerciseWithProgress,
    onClick: () -> Unit
) {
    // Запускаем анимации при появлении
    var animationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100) // Небольшая задержка для эффекта
        animationStarted = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая часть: название и статистика
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exerciseData.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Рабочий вес с анимацией
                    Column {
                        Text(
                            "Рабочий вес",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        AnimatedNumber(
                            targetValue = if (animationStarted) exerciseData.lastWorkingWeight.toInt() else 0,
                            suffix = " кг"
                        )
                    }

                    // 1RM с анимацией
                    Column {
                        Text(
                            "1RM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        AnimatedNumber(
                            targetValue = if (animationStarted) exerciseData.last1RM.toInt() else 0,
                            suffix = " кг",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "${exerciseData.workoutCount} тренировок",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Правая часть: мини-график
            if (exerciseData.progressHistory.isNotEmpty()) {
                MiniProgressChart(
                    progress = exerciseData.progressHistory,
                    animate = animationStarted,
                    modifier = Modifier.size(80.dp, 50.dp)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Открыть",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun AnimatedNumber(
    targetValue: Int,
    suffix: String = "",
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "number"
    )

    Text(
        text = "$animatedValue$suffix",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
fun MiniProgressChart(
    progress: List<WeightProgress>,
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Анимация отрисовки графика
    val animationProgress by animateFloatAsState(
        targetValue = if (animate) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "chartAnimation"
    )

    if (progress.isEmpty()) return

    val values = progress.map { it.estimated1RM.toFloat() }
    val minValue = values.minOrNull() ?: 0f
    val maxValue = values.maxOrNull() ?: 1f
    val range = (maxValue - minValue).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)
        val padding = 4.dp.toPx()
        val chartHeight = size.height - padding * 2

        // Количество точек для отрисовки на основе анимации
        val pointsToDraw = (values.size * animationProgress).toInt().coerceAtLeast(1)

        val path = Path()
        var started = false

        for (i in 0 until pointsToDraw) {
            val x = i * stepX
            val normalizedY = (values[i] - minValue) / range
            val y = size.height - padding - (normalizedY * chartHeight)

            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // Рисуем точки
        for (i in 0 until pointsToDraw) {
            val x = i * stepX
            val normalizedY = (values[i] - minValue) / range
            val y = size.height - padding - (normalizedY * chartHeight)

            drawCircle(
                color = primaryColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun AnimatedStatsCard(progress: List<WeightProgress>) {
    var animationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        animationStarted = false
        delay(100)
        animationStarted = true
    }

    val max1RM = progress.maxOfOrNull { it.estimated1RM }?.toInt() ?: 0
    val min1RM = progress.minOfOrNull { it.estimated1RM }?.toInt() ?: 0
    val last1RM = progress.lastOrNull()?.estimated1RM?.toInt() ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Расчётный 1RM (формула Эпли)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnimatedStatItem(
                    label = "Макс.",
                    targetValue = if (animationStarted) max1RM else 0,
                    suffix = " кг"
                )
                AnimatedStatItem(
                    label = "Мин.",
                    targetValue = if (animationStarted) min1RM else 0,
                    suffix = " кг"
                )
                AnimatedStatItem(
                    label = "Последний",
                    targetValue = if (animationStarted) last1RM else 0,
                    suffix = " кг"
                )
            }
        }
    }
}

@Composable
fun AnimatedStatItem(
    label: String,
    targetValue: Int,
    suffix: String
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "statValue"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            "$animatedValue$suffix",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyStateMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun ProgressHistoryTable(progress: List<WeightProgress>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок таблицы
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Дата",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "Подход",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "1RM",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            val formatter = DateTimeFormatter.ofPattern("dd.MM")
            progress.takeLast(10).reversed().forEach { entry ->
                val date = LocalDate.parse(entry.date)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        date.format(formatter),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${entry.bestWeight.toInt()}×${entry.bestReps}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${entry.estimated1RM.toInt()} кг",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun WeightProgressChart(
    progress: List<WeightProgress>,
    modifier: Modifier = Modifier
) {
    if (progress.isEmpty()) return

    // Используем расчётный 1RM для графика
    val entries = progress.mapIndexed { index, wp ->
        index.toFloat() to wp.estimated1RM.toFloat()
    }

    val chartEntryModel = entryModelOf(*entries.toTypedArray())

    val dateLabels = progress.map { wp ->
        val date = LocalDate.parse(wp.date)
        "${date.dayOfMonth}/${date.monthValue}"
    }

    val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        dateLabels.getOrElse(value.toInt()) { "" }
    }

    Chart(
        chart = lineChart(),
        model = chartEntryModel,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisValueFormatter),
        modifier = modifier,
        chartScrollState = rememberChartScrollState()
    )
}
