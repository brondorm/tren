package com.gymtracker.ui.screens

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
import androidx.compose.ui.unit.dp
import com.gymtracker.data.model.Exercise
import com.gymtracker.data.model.MuscleGroup
import com.gymtracker.data.repository.GymRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    repository: GymRepository,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val exercises by repository.allExercises.collectAsState(initial = emptyList())
    val muscleGroups by repository.allMuscleGroups.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    
    val muscleGroupsMap = muscleGroups.associateBy { it.id }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Упражнения") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Добавить упражнение")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(exercises) { exercise ->
                val muscleGroup = exercise.muscleGroupId?.let { muscleGroupsMap[it] }
                val synergist1 = exercise.synergistMuscleGroupId?.let { muscleGroupsMap[it] }
                val synergist2 = exercise.synergistMuscleGroupId2?.let { muscleGroupsMap[it] }
                ExerciseItem(
                    exercise = exercise,
                    muscleGroup = muscleGroup,
                    synergist1 = synergist1,
                    synergist2 = synergist2,
                    onClick = { onNavigateToEdit(exercise.id) }
                )
            }
        }
    }
    
    if (showAddDialog) {
        AddExerciseDialog(
            muscleGroups = muscleGroups,
            onDismiss = { showAddDialog = false },
            onAdd = { name, muscleGroupId ->
                showAddDialog = false
                onNavigateToEdit(-1) // -1 означает новое упражнение, но мы сначала создадим его
            },
            repository = repository
        )
    }
}

@Composable
fun ExerciseItem(
    exercise: Exercise,
    muscleGroup: MuscleGroup?,
    synergist1: MuscleGroup? = null,
    synergist2: MuscleGroup? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (muscleGroup != null) {
                    val synergists = listOfNotNull(synergist1, synergist2)
                    val muscleText = if (synergists.isNotEmpty()) {
                        "${muscleGroup.name} + ${synergists.joinToString(", ") { it.name }}"
                    } else {
                        muscleGroup.name
                    }
                    Text(
                        text = muscleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        text = "Не привязано",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    muscleGroups: List<MuscleGroup>,
    onDismiss: () -> Unit,
    onAdd: (String, Long?) -> Unit,
    repository: GymRepository
) {
    var name by remember { mutableStateOf("") }
    var selectedMuscleGroupId by remember { mutableStateOf<Long?>(null) }
    var selectedSynergistId by remember { mutableStateOf<Long?>(null) }
    var selectedSynergistId2 by remember { mutableStateOf<Long?>(null) }
    var expandedMain by remember { mutableStateOf(false) }
    var expandedSynergist by remember { mutableStateOf(false) }
    var expandedSynergist2 by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val selectedMuscleGroup = muscleGroups.find { it.id == selectedMuscleGroupId }
    val selectedSynergist = muscleGroups.find { it.id == selectedSynergistId }
    val selectedSynergist2 = muscleGroups.find { it.id == selectedSynergistId2 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое упражнение") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Основная группа мышц
                Text(
                    "Основная мышца",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedMain,
                    onExpandedChange = { expandedMain = it }
                ) {
                    OutlinedTextField(
                        value = selectedMuscleGroup?.name ?: "Выберите группу мышц",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMain) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedMain,
                        onDismissRequest = { expandedMain = false }
                    ) {
                        muscleGroups.forEach { mg ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (mg.parentId != null) "  ${mg.name}" else mg.name
                                    )
                                },
                                onClick = {
                                    selectedMuscleGroupId = mg.id
                                    expandedMain = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Синергист 1 (опционально)
                Text(
                    "Синергист 1 (опционально)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "Вспомогательная мышца, считается как 0.5 подхода",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedSynergist,
                    onExpandedChange = { expandedSynergist = it }
                ) {
                    OutlinedTextField(
                        value = selectedSynergist?.name ?: "Не выбрано",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSynergist) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedSynergist,
                        onDismissRequest = { expandedSynergist = false }
                    ) {
                        // Опция "Не выбрано"
                        DropdownMenuItem(
                            text = { Text("Не выбрано") },
                            onClick = {
                                selectedSynergistId = null
                                expandedSynergist = false
                            }
                        )

                        HorizontalDivider()

                        muscleGroups.forEach { mg ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (mg.parentId != null) "  ${mg.name}" else mg.name
                                    )
                                },
                                onClick = {
                                    selectedSynergistId = mg.id
                                    expandedSynergist = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Синергист 2 (опционально)
                Text(
                    "Синергист 2 (опционально)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedSynergist2,
                    onExpandedChange = { expandedSynergist2 = it }
                ) {
                    OutlinedTextField(
                        value = selectedSynergist2?.name ?: "Не выбрано",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSynergist2) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedSynergist2,
                        onDismissRequest = { expandedSynergist2 = false }
                    ) {
                        // Опция "Не выбрано"
                        DropdownMenuItem(
                            text = { Text("Не выбрано") },
                            onClick = {
                                selectedSynergistId2 = null
                                expandedSynergist2 = false
                            }
                        )

                        HorizontalDivider()

                        muscleGroups.forEach { mg ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (mg.parentId != null) "  ${mg.name}" else mg.name
                                    )
                                },
                                onClick = {
                                    selectedSynergistId2 = mg.id
                                    expandedSynergist2 = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        scope.launch {
                            repository.insertExercise(
                                Exercise(
                                    name = name,
                                    muscleGroupId = selectedMuscleGroupId,
                                    synergistMuscleGroupId = selectedSynergistId,
                                    synergistMuscleGroupId2 = selectedSynergistId2
                                )
                            )
                            onDismiss()
                        }
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseScreen(
    repository: GymRepository,
    exerciseId: Long,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val muscleGroups by repository.allMuscleGroups.collectAsState(initial = emptyList())

    var exercise by remember { mutableStateOf<Exercise?>(null) }
    var name by remember { mutableStateOf("") }
    var selectedMuscleGroupId by remember { mutableStateOf<Long?>(null) }
    var selectedSynergistId by remember { mutableStateOf<Long?>(null) }
    var selectedSynergistId2 by remember { mutableStateOf<Long?>(null) }
    var expandedMain by remember { mutableStateOf(false) }
    var expandedSynergist by remember { mutableStateOf(false) }
    var expandedSynergist2 by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(exerciseId) {
        if (exerciseId > 0) {
            exercise = repository.getExerciseById(exerciseId)
            exercise?.let {
                name = it.name
                selectedMuscleGroupId = it.muscleGroupId
                selectedSynergistId = it.synergistMuscleGroupId
                selectedSynergistId2 = it.synergistMuscleGroupId2
            }
        }
    }

    val selectedMuscleGroup = muscleGroups.find { it.id == selectedMuscleGroupId }
    val selectedSynergist = muscleGroups.find { it.id == selectedSynergistId }
    val selectedSynergist2 = muscleGroups.find { it.id == selectedSynergistId2 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (exerciseId > 0) "Редактирование" else "Новое упражнение") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (exerciseId > 0) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (name.isNotBlank()) {
                                    if (exerciseId > 0 && exercise != null) {
                                        repository.updateExercise(
                                            exercise!!.copy(
                                                name = name,
                                                muscleGroupId = selectedMuscleGroupId,
                                                synergistMuscleGroupId = selectedSynergistId,
                                                synergistMuscleGroupId2 = selectedSynergistId2
                                            )
                                        )
                                    } else {
                                        repository.insertExercise(
                                            Exercise(
                                                name = name,
                                                muscleGroupId = selectedMuscleGroupId,
                                                synergistMuscleGroupId = selectedSynergistId,
                                                synergistMuscleGroupId2 = selectedSynergistId2
                                            )
                                        )
                                    }
                                    onNavigateBack()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, "Сохранить")
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Основная группа мышц
            Text(
                "Основная мышца",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expandedMain,
                onExpandedChange = { expandedMain = it }
            ) {
                OutlinedTextField(
                    value = selectedMuscleGroup?.name ?: "Не выбрано",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMain) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedMain,
                    onDismissRequest = { expandedMain = false }
                ) {
                    // Опция "Не выбрано"
                    DropdownMenuItem(
                        text = { Text("Не выбрано") },
                        onClick = {
                            selectedMuscleGroupId = null
                            expandedMain = false
                        }
                    )

                    HorizontalDivider()

                    muscleGroups.forEach { mg ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (mg.parentId != null) "  ${mg.name}" else mg.name,
                                    style = if (mg.parentId == null)
                                        MaterialTheme.typography.titleSmall
                                    else
                                        MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                selectedMuscleGroupId = mg.id
                                expandedMain = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Синергист 1 (опционально)
            Text(
                "Синергист 1 (опционально)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                "Вспомогательная мышца, считается как 0.5 подхода",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expandedSynergist,
                onExpandedChange = { expandedSynergist = it }
            ) {
                OutlinedTextField(
                    value = selectedSynergist?.name ?: "Не выбрано",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSynergist) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedSynergist,
                    onDismissRequest = { expandedSynergist = false }
                ) {
                    // Опция "Не выбрано"
                    DropdownMenuItem(
                        text = { Text("Не выбрано") },
                        onClick = {
                            selectedSynergistId = null
                            expandedSynergist = false
                        }
                    )

                    HorizontalDivider()

                    muscleGroups.forEach { mg ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (mg.parentId != null) "  ${mg.name}" else mg.name,
                                    style = if (mg.parentId == null)
                                        MaterialTheme.typography.titleSmall
                                    else
                                        MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                selectedSynergistId = mg.id
                                expandedSynergist = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Синергист 2 (опционально)
            Text(
                "Синергист 2 (опционально)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expandedSynergist2,
                onExpandedChange = { expandedSynergist2 = it }
            ) {
                OutlinedTextField(
                    value = selectedSynergist2?.name ?: "Не выбрано",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSynergist2) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedSynergist2,
                    onDismissRequest = { expandedSynergist2 = false }
                ) {
                    // Опция "Не выбрано"
                    DropdownMenuItem(
                        text = { Text("Не выбрано") },
                        onClick = {
                            selectedSynergistId2 = null
                            expandedSynergist2 = false
                        }
                    )

                    HorizontalDivider()

                    muscleGroups.forEach { mg ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (mg.parentId != null) "  ${mg.name}" else mg.name,
                                    style = if (mg.parentId == null)
                                        MaterialTheme.typography.titleSmall
                                    else
                                        MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                selectedSynergistId2 = mg.id
                                expandedSynergist2 = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить упражнение?") },
            text = { Text("Это действие нельзя отменить. Все записи с этим упражнением будут удалены.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            exercise?.let { repository.deleteExercise(it) }
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
