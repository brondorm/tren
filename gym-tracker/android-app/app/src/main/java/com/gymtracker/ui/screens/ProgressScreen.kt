package com.gymtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gymtracker.data.database.WeightProgress
import com.gymtracker.data.model.Exercise
import com.gymtracker.data.repository.GymRepository
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.entryModelOf
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
    var isLoading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Выбор упражнения
            Text(
                "Выберите упражнение",
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
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (selectedExercise == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Выберите упражнение для просмотра прогресса",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (progress.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Нет данных для этого упражнения",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                // Статистика
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        val maxWeight = progress.maxOfOrNull { it.maxWeight } ?: 0.0
                        val minWeight = progress.minOfOrNull { it.maxWeight } ?: 0.0
                        val lastWeight = progress.lastOrNull()?.maxWeight ?: 0.0
                        
                        StatItem(label = "Макс.", value = "${maxWeight.toInt()} кг")
                        StatItem(label = "Мин.", value = "${minWeight.toInt()} кг")
                        StatItem(label = "Последний", value = "${lastWeight.toInt()} кг")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Динамика веса",
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Таблица данных
                Text(
                    "История",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
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
                                    "${entry.maxWeight} кг",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
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
    
    val entries = progress.mapIndexed { index, wp ->
        index.toFloat() to wp.maxWeight.toFloat()
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
