package com.gymtracker.data.model

/**
 * Модели для импорта/экспорта данных в JSON формате
 */

data class ExportData(
    val muscle_groups: List<MuscleGroupJson>,
    val exercises: List<ExerciseJson>,
    val workouts: List<WorkoutJson>
)

data class MuscleGroupJson(
    val id: Long,
    val name: String,
    val parent_id: Long?
)

data class ExerciseJson(
    val name: String,
    val muscle_group: String?
)

data class WorkoutJson(
    val date: String,
    val entries: List<WorkoutEntryJson>
)

data class WorkoutEntryJson(
    val exercise: String,
    val sets: List<SetJson>
)

data class SetJson(
    val set_number: Int,
    val weight: Double,
    val reps: Int,
    val note: String?
)

// Расширения для конвертации

fun MuscleGroup.toJson() = MuscleGroupJson(id, name, parentId)

fun MuscleGroupJson.toEntity() = MuscleGroup(id, name, parent_id)

fun Exercise.toJson(muscleGroupName: String?) = ExerciseJson(name, muscleGroupName)
