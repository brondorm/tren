package com.gymtracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Группа мышц (Грудь, Спина, Широчайшие и т.д.)
 * Поддерживает иерархию через parentId
 */
@Entity(tableName = "muscle_groups")
data class MuscleGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null
)

/**
 * Упражнение (Жим, Подтягивания и т.д.)
 * Привязано к группе мышц
 */
@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = MuscleGroup::class,
            parentColumns = ["id"],
            childColumns = ["muscleGroupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("muscleGroupId")]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val muscleGroupId: Long? = null
)

/**
 * Тренировка (один день)
 */
@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String // формат YYYY-MM-DD
)

/**
 * Запись упражнения в тренировке
 * Связывает тренировку с упражнением
 */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId"), Index("exerciseId")]
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val orderIndex: Int = 0
)

/**
 * Подход (сет)
 */
@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutExerciseId")]
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutExerciseId: Long,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val note: String? = null
)
