package com.gymtracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymtracker.data.database.MuscleGroupStats
import com.gymtracker.data.repository.GymRepository
import com.gymtracker.ui.components.MuscleGroupIcon
import com.gymtracker.ui.components.getMuscleGroupColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

enum class StatsPeriod(val label: String) {
    WEEK("Неделя"),
    MONTH("Месяц"),
    YEAR("Год")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    repository: GymRepository,
    onNavigateBack: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(StatsPeriod.WEEK) }
    var stats by remember { mutableStateOf<List<MuscleGroupStats>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(selectedPeriod) {
        isLoading = true
        val today = LocalDate.now()
        val (startDate, endDate) = when (selectedPeriod) {
            StatsPeriod.WEEK -> {
                val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                weekStart to weekStart.plusDays(6)
            }
            StatsPeriod.MONTH -> {
                val monthStart = today.withDayOfMonth(1)
                monthStart to today
            }
            StatsPeriod.YEAR -> {
                val yearStart = today.withDayOfYear(1)
                yearStart to today
            }
        }
        stats = repository.getMuscleStatsByRange(startDate.toString(), endDate.toString())
        isLoading = false
    }
    
    val totalSets = stats.sumOf { it.totalSets }
    val totalReps = stats.sumOf { it.totalReps }

    // Форматирование дробных подходов
    fun formatSets(sets: Double): String {
        return if (sets == sets.toLong().toDouble()) {
            sets.toLong().toString()
        } else {
            String.format("%.1f", sets)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Выбор периода
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.label) }
                    )
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (stats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PieChart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Нет данных за выбранный период",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                // Общая статистика
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "Подходов", value = formatSets(totalSets))
                        StatItem(label = "Повторений", value = totalReps.toString())
                        StatItem(label = "Групп мышц", value = stats.size.toString())
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Статистика по группам мышц
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(stats) { index, stat ->
                        AnimatedMuscleGroupStatCard(
                            stat = stat,
                            index = index,
                            maxSets = stats.maxOfOrNull { it.totalSets } ?: 1.0,
                            formatSets = ::formatSets
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun AnimatedMuscleGroupStatCard(
    stat: MuscleGroupStats,
    index: Int,
    maxSets: Double,
    formatSets: (Double) -> String
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(stat.muscleGroup) {
        delay(index * 60L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + slideInHorizontally(
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            initialOffsetX = { -it / 4 }
        ) + scaleIn(
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            initialScale = 0.9f
        )
    ) {
        MuscleGroupStatCard(
            stat = stat,
            maxSets = maxSets,
            formatSets = formatSets
        )
    }
}

@Composable
fun MuscleGroupStatCard(
    stat: MuscleGroupStats,
    maxSets: Double,
    formatSets: (Double) -> String
) {
    val progress = (stat.totalSets / maxSets).toFloat()
    val muscleColor = getMuscleGroupColor(stat.muscleGroup)

    // Анимация прогресс-бара
    var animatedProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(progress) {
        animatedProgress = 0f
        delay(100)
        animatedProgress = progress
    }
    val animatedProgressValue by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка группы мышц
            MuscleGroupIcon(
                muscleGroupName = stat.muscleGroup,
                size = 48.dp,
                iconSize = 28.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stat.muscleGroup,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${formatSets(stat.totalSets)} подх.",
                        style = MaterialTheme.typography.titleMedium,
                        color = muscleColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${stat.totalReps} повторений",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { animatedProgressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = muscleColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
