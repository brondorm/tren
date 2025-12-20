package com.gymtracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymtracker.data.model.Exercise
import com.gymtracker.data.model.ExerciseSet
import com.gymtracker.data.repository.GymRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

// Данные подхода для UI
data class SetData(
    val setNumber: Int,
    var weight: String = "",
    var reps: String = "",
    var note: String = ""
)

// Запись упражнения для UI
data class ExerciseEntryData(
    val exercise: Exercise,
    val sets: MutableList<SetData> = mutableStateListOf()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutScreen(
    repository: GymRepository,
    date: String,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val exercises by repository.allExercises.collectAsState(initial = emptyList())
    
    // Состояние тренировки
    val entries = remember { mutableStateListOf<ExerciseEntryData>() }
    var showExerciseDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    // Загружаем существующую тренировку или позапрошлую (для системы тяни-толкай)
    LaunchedEffect(date) {
        val existingWorkout = repository.getWorkoutByDate(date)
        if (existingWorkout != null) {
            // Редактируем существующую тренировку
            val fullWorkout = repository.getFullWorkout(existingWorkout.id)
            fullWorkout?.entries?.forEach { entry ->
                if (entry.exercise != null) {
                    val setDataList = entry.sets.map { set ->
                        SetData(
                            setNumber = set.setNumber,
                            weight = set.weight.toString(),
                            reps = set.reps.toString(),
                            note = set.note ?: ""
                        )
                    }.toMutableList()
                    entries.add(ExerciseEntryData(entry.exercise, setDataList.toMutableStateList()))
                }
            }
        } else {
            // Новая тренировка - загружаем позапрошлую (для системы тяни-толкай)
            val secondPrevWorkout = repository.getSecondPreviousWorkout(date)
            if (secondPrevWorkout != null) {
                val fullWorkout = repository.getFullWorkout(secondPrevWorkout.id)
                fullWorkout?.entries?.forEach { entry ->
                    if (entry.exercise != null) {
                        val setDataList = entry.sets.map { set ->
                            SetData(
                                setNumber = set.setNumber,
                                weight = set.weight.toString(),
                                reps = set.reps.toString(),
                                note = set.note ?: ""
                            )
                        }.toMutableList()
                        entries.add(ExerciseEntryData(entry.exercise, setDataList.toMutableStateList()))
                    }
                }
            }
        }
        isLoading = false
    }
    
    val localDate = LocalDate.parse(date)
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localDate.format(formatter)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    saveWorkout(repository, date, entries)
                                    isSaving = false
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Save, "Сохранить")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showExerciseDialog = true }
            ) {
                Icon(Icons.Default.Add, "Добавить упражнение")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Нет упражнений",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Нажмите + чтобы добавить",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(entries) { index, entry ->
                    ExerciseCard(
                        entry = entry,
                        onRemove = { entries.removeAt(index) },
                        onAddSet = {
                            val previousWeight = entry.sets.lastOrNull()?.weight ?: ""
                            entry.sets.add(SetData(setNumber = entry.sets.size + 1, weight = previousWeight))
                        },
                        onRemoveSet = { setIndex ->
                            if (entry.sets.size > 1) {
                                entry.sets.removeAt(setIndex)
                                // Перенумеровываем
                                entry.sets.forEachIndexed { i, set ->
                                    entry.sets[i] = set.copy(setNumber = i + 1)
                                }
                            }
                        }
                    )
                }
                
                // Отступ для FAB
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
    
    if (showExerciseDialog) {
        ExerciseSelectDialog(
            exercises = exercises,
            onDismiss = { showExerciseDialog = false },
            onSelect = { exercise ->
                showExerciseDialog = false
                val newEntry = ExerciseEntryData(
                    exercise = exercise,
                    sets = mutableStateListOf(SetData(setNumber = 1))
                )
                entries.add(newEntry)
            }
        )
    }
}

@Composable
fun ExerciseCard(
    entry: ExerciseEntryData,
    onRemove: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок упражнения
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.exercise.name,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Заголовки колонок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "#",
                    modifier = Modifier.width(32.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "Вес (кг)",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "Повторы",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(40.dp))
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Подходы
            entry.sets.forEachIndexed { index, set ->
                SetRow(
                    set = set,
                    onWeightChange = { entry.sets[index] = set.copy(weight = it) },
                    onRepsChange = { entry.sets[index] = set.copy(reps = it) },
                    onRemove = { onRemoveSet(index) },
                    canRemove = entry.sets.size > 1
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            // Кнопка добавления подхода
            TextButton(
                onClick = onAddSet,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Добавить подход")
            }
        }
    }
}

@Composable
fun SetRow(
    set: SetData,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${set.setNumber}",
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        
        OutlinedTextField(
            value = set.weight,
            onValueChange = onWeightChange,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
        
        OutlinedTextField(
            value = set.reps,
            onValueChange = onRepsChange,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
        
        IconButton(
            onClick = onRemove,
            enabled = canRemove
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Удалить подход",
                tint = if (canRemove) MaterialTheme.colorScheme.outline 
                       else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onSelect: (Exercise) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredExercises = exercises.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите упражнение") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(filteredExercises.size) { index ->
                        val exercise = filteredExercises[index]
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            modifier = Modifier.clickable { onSelect(exercise) }
                        )
                        if (index < filteredExercises.size - 1) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private suspend fun saveWorkout(
    repository: GymRepository,
    date: String,
    entries: List<ExerciseEntryData>
) {
    val dataToSave = entries.mapNotNull { entry ->
        val sets = entry.sets.mapNotNull { set ->
            val weight = set.weight.toDoubleOrNull()
            val reps = set.reps.toIntOrNull()
            if (weight != null && reps != null) {
                ExerciseSet(
                    workoutExerciseId = 0, // будет установлен в репозитории
                    setNumber = set.setNumber,
                    weight = weight,
                    reps = reps,
                    note = set.note.ifBlank { null }
                )
            } else null
        }

        if (sets.isNotEmpty()) {
            entry.exercise.id to sets
        } else null
    }

    if (dataToSave.isNotEmpty()) {
        repository.saveFullWorkout(date, dataToSave)
    } else {
        // Если нет данных для сохранения, удаляем тренировку
        val workout = repository.getWorkoutByDate(date)
        if (workout != null) {
            repository.deleteWorkout(workout)
        }
    }
}

// Расширение для создания mutableStateList
private fun <T> MutableList<T>.toMutableStateList(): MutableList<T> {
    return mutableStateListOf<T>().also { it.addAll(this) }
}
