package com.gymtracker.data.database

import androidx.room.*
import com.gymtracker.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MuscleGroupDao {
    // Исправлено: заменено "NULLS FIRST" на CASE-выражение для совместимости
    @Query("SELECT * FROM muscle_groups ORDER BY CASE WHEN parentId IS NULL THEN 0 ELSE 1 END, parentId, name")
    fun getAllFlow(): Flow<List<MuscleGroup>>

    // Исправлено: заменено "NULLS FIRST" на CASE-выражение для совместимости
    @Query("SELECT * FROM muscle_groups ORDER BY CASE WHEN parentId IS NULL THEN 0 ELSE 1 END, parentId, name")
    suspend fun getAll(): List<MuscleGroup>

    @Query("SELECT * FROM muscle_groups WHERE id = :id")
    suspend fun getById(id: Long): MuscleGroup?

    @Query("SELECT * FROM muscle_groups WHERE parentId IS NULL")
    suspend fun getRootGroups(): List<MuscleGroup>

    @Query("SELECT * FROM muscle_groups WHERE parentId = :parentId")
    suspend fun getChildGroups(parentId: Long): List<MuscleGroup>

    @Insert
    suspend fun insert(muscleGroup: MuscleGroup): Long

    @Insert
    suspend fun insertAll(muscleGroups: List<MuscleGroup>)

    @Update
    suspend fun update(muscleGroup: MuscleGroup)

    @Delete
    suspend fun delete(muscleGroup: MuscleGroup)

    @Query("DELETE FROM muscle_groups")
    suspend fun deleteAll()
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name")
    fun getAllFlow(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises ORDER BY name")
    suspend fun getAll(): List<Exercise>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Exercise?

    @Query("SELECT * FROM exercises WHERE muscleGroupId = :muscleGroupId")
    suspend fun getByMuscleGroup(muscleGroupId: Long): List<Exercise>

    @Query("SELECT * FROM exercises WHERE muscleGroupId IS NULL")
    suspend fun getUnmapped(): List<Exercise>

    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Insert
    suspend fun insertAll(exercises: List<Exercise>)

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun getAllFlow(): Flow<List<Workout>>

    @Query("SELECT * FROM workouts ORDER BY date DESC")
    suspend fun getAll(): List<Workout>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: Long): Workout?

    @Query("SELECT * FROM workouts WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): Workout?

    @Query("SELECT * FROM workouts WHERE date LIKE :yearMonth || '%' ORDER BY date")
    suspend fun getByMonth(yearMonth: String): List<Workout>

    @Query("SELECT * FROM workouts WHERE date LIKE :year || '%' ORDER BY date")
    suspend fun getByYear(year: String): List<Workout>

    @Query("SELECT * FROM workouts WHERE date < :beforeDate ORDER BY date DESC LIMIT :limit")
    suspend fun getPreviousWorkouts(beforeDate: String, limit: Int): List<Workout>

    @Insert
    suspend fun insert(workout: Workout): Long

    @Update
    suspend fun update(workout: Workout)

    @Delete
    suspend fun delete(workout: Workout)

    @Query("DELETE FROM workouts")
    suspend fun deleteAll()
}

@Dao
interface WorkoutExerciseDao {
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex")
    suspend fun getByWorkout(workoutId: Long): List<WorkoutExercise>

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getById(id: Long): WorkoutExercise?

    @Insert
    suspend fun insert(workoutExercise: WorkoutExercise): Long

    @Update
    suspend fun update(workoutExercise: WorkoutExercise)

    @Delete
    suspend fun delete(workoutExercise: WorkoutExercise)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteByWorkout(workoutId: Long)
}

@Dao
interface ExerciseSetDao {
    @Query("SELECT * FROM sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber")
    suspend fun getByWorkoutExercise(workoutExerciseId: Long): List<ExerciseSet>

    @Query("SELECT * FROM sets WHERE id = :id")
    suspend fun getById(id: Long): ExerciseSet?

    @Insert
    suspend fun insert(set: ExerciseSet): Long

    @Insert
    suspend fun insertAll(sets: List<ExerciseSet>)

    @Update
    suspend fun update(set: ExerciseSet)

    @Delete
    suspend fun delete(set: ExerciseSet)

    @Query("DELETE FROM sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun deleteByWorkoutExercise(workoutExerciseId: Long)
}

// Комбинированные запросы для статистики
@Dao
interface StatsDao {
    /**
     * Получает суммарные подходы по группам мышц за период
     */
    @Query("""
        SELECT mg.name as muscleGroup, COUNT(s.id) as totalSets, SUM(s.reps) as totalReps
        FROM sets s
        INNER JOIN workout_exercises we ON s.workoutExerciseId = we.id
        INNER JOIN workouts w ON we.workoutId = w.id
        INNER JOIN exercises e ON we.exerciseId = e.id
        INNER JOIN muscle_groups mg ON e.muscleGroupId = mg.id
        WHERE w.date BETWEEN :startDate AND :endDate
        GROUP BY mg.id
        ORDER BY totalSets DESC
    """)
    suspend fun getStatsByMuscleGroup(startDate: String, endDate: String): List<MuscleGroupStats>

    /**
     * Получает прогресс веса по упражнению
     */
    @Query("""
        SELECT w.date, MAX(s.weight) as maxWeight
        FROM sets s
        INNER JOIN workout_exercises we ON s.workoutExerciseId = we.id
        INNER JOIN workouts w ON we.workoutId = w.id
        WHERE we.exerciseId = :exerciseId
        GROUP BY w.date
        ORDER BY w.date
    """)
    suspend fun getWeightProgress(exerciseId: Long): List<WeightProgress>

    /**
     * Получает прогресс веса по упражнению за период
     */
    @Query("""
        SELECT w.date, MAX(s.weight) as maxWeight
        FROM sets s
        INNER JOIN workout_exercises we ON s.workoutExerciseId = we.id
        INNER JOIN workouts w ON we.workoutId = w.id
        WHERE we.exerciseId = :exerciseId 
          AND w.date BETWEEN :startDate AND :endDate
        GROUP BY w.date
        ORDER BY w.date
    """)
    suspend fun getWeightProgressInRange(exerciseId: Long, startDate: String, endDate: String): List<WeightProgress>
}

// Data classes для результатов запросов
data class MuscleGroupStats(
    val muscleGroup: String,
    val totalSets: Int,
    val totalReps: Int
)

data class WeightProgress(
    val date: String,
    val maxWeight: Double
)
