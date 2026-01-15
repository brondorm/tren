package com.gymtracker.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.gymtracker.data.database.*
import com.gymtracker.data.model.*
import kotlinx.coroutines.flow.Flow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class GymRepository(private val database: GymDatabase) {
    
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    // ===== Muscle Groups =====
    
    val allMuscleGroups: Flow<List<MuscleGroup>> = database.muscleGroupDao().getAllFlow()
    
    suspend fun getMuscleGroups(): List<MuscleGroup> = database.muscleGroupDao().getAll()
    
    suspend fun getMuscleGroupById(id: Long): MuscleGroup? = database.muscleGroupDao().getById(id)
    
    suspend fun insertMuscleGroup(muscleGroup: MuscleGroup): Long = 
        database.muscleGroupDao().insert(muscleGroup)
    
    suspend fun updateMuscleGroup(muscleGroup: MuscleGroup) = 
        database.muscleGroupDao().update(muscleGroup)
    
    suspend fun deleteMuscleGroup(muscleGroup: MuscleGroup) = 
        database.muscleGroupDao().delete(muscleGroup)
    
    // ===== Exercises =====
    
    val allExercises: Flow<List<Exercise>> = database.exerciseDao().getAllFlow()
    
    suspend fun getExercises(): List<Exercise> = database.exerciseDao().getAll()
    
    suspend fun getExerciseById(id: Long): Exercise? = database.exerciseDao().getById(id)
    
    suspend fun getExerciseByName(name: String): Exercise? = database.exerciseDao().getByName(name)
    
    suspend fun getUnmappedExercises(): List<Exercise> = database.exerciseDao().getUnmapped()
    
    suspend fun insertExercise(exercise: Exercise): Long = database.exerciseDao().insert(exercise)
    
    suspend fun updateExercise(exercise: Exercise) = database.exerciseDao().update(exercise)
    
    suspend fun deleteExercise(exercise: Exercise) = database.exerciseDao().delete(exercise)
    
    // ===== Workouts =====
    
    val allWorkouts: Flow<List<Workout>> = database.workoutDao().getAllFlow()
    
    suspend fun getWorkouts(): List<Workout> = database.workoutDao().getAll()
    
    suspend fun getWorkoutById(id: Long): Workout? = database.workoutDao().getById(id)
    
    suspend fun getWorkoutByDate(date: String): Workout? = database.workoutDao().getByDate(date)
    
    suspend fun getWorkoutsByMonth(yearMonth: String): List<Workout> = 
        database.workoutDao().getByMonth(yearMonth)
    
    suspend fun insertWorkout(workout: Workout): Long = database.workoutDao().insert(workout)
    
    suspend fun updateWorkout(workout: Workout) = database.workoutDao().update(workout)
    
    suspend fun deleteWorkout(workout: Workout) = database.workoutDao().delete(workout)

    /**
     * Получает позапрошлую тренировку (вторую по дате перед указанной)
     * Используется для системы тяни-толкай, где чередуются типы тренировок
     */
    suspend fun getSecondPreviousWorkout(beforeDate: String): Workout? {
        val previous = database.workoutDao().getPreviousWorkouts(beforeDate, 2)
        return previous.getOrNull(1) // Индекс 1 = вторая (позапрошлая)
    }
    
    // ===== Workout Exercises =====
    
    suspend fun getWorkoutExercises(workoutId: Long): List<WorkoutExercise> = 
        database.workoutExerciseDao().getByWorkout(workoutId)
    
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise): Long = 
        database.workoutExerciseDao().insert(workoutExercise)
    
    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise) = 
        database.workoutExerciseDao().update(workoutExercise)
    
    suspend fun deleteWorkoutExercise(workoutExercise: WorkoutExercise) = 
        database.workoutExerciseDao().delete(workoutExercise)
    
    // ===== Sets =====
    
    suspend fun getSets(workoutExerciseId: Long): List<ExerciseSet> = 
        database.exerciseSetDao().getByWorkoutExercise(workoutExerciseId)
    
    suspend fun insertSet(set: ExerciseSet): Long = database.exerciseSetDao().insert(set)
    
    suspend fun insertSets(sets: List<ExerciseSet>) = database.exerciseSetDao().insertAll(sets)
    
    suspend fun updateSet(set: ExerciseSet) = database.exerciseSetDao().update(set)
    
    suspend fun deleteSet(set: ExerciseSet) = database.exerciseSetDao().delete(set)

    /**
     * Получает последние подходы для упражнения из предыдущей тренировки
     * Используется для предзаполнения веса при добавлении упражнения
     */
    suspend fun getLastSetsForExercise(exerciseId: Long): List<LastSetData> =
        database.statsDao().getLastSetsForExercise(exerciseId)

    // ===== Clear All Data =====

    suspend fun clearAllData() {
        database.workoutDao().deleteAll()
        database.exerciseDao().deleteAll()
        database.muscleGroupDao().deleteAll()
    }

    // ===== Statistics =====
    
    suspend fun getWeeklyMuscleStats(): List<MuscleGroupStats> {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        return database.statsDao().getStatsByMuscleGroup(
            weekStart.toString(), 
            weekEnd.toString()
        )
    }
    
    suspend fun getMuscleStatsByRange(startDate: String, endDate: String): List<MuscleGroupStats> = 
        database.statsDao().getStatsByMuscleGroup(startDate, endDate)
    
    /**
     * Рассчитывает прогресс по упражнению используя формулу Эпли для 1RM
     * Формула Эпли: 1RM = weight × (1 + reps/30)
     * За каждый день выбирается лучший подход по расчётному 1RM
     */
    suspend fun getWeightProgress(exerciseId: Long): List<WeightProgress> {
        val setsWithDates = database.statsDao().getExerciseSetsWithDates(exerciseId)
        return calculateBestSetsByDate(setsWithDates)
    }

    suspend fun getWeightProgressByRange(
        exerciseId: Long,
        startDate: String,
        endDate: String
    ): List<WeightProgress> {
        val setsWithDates = database.statsDao().getExerciseSetsWithDatesInRange(exerciseId, startDate, endDate)
        return calculateBestSetsByDate(setsWithDates)
    }

    /**
     * Группирует подходы по датам и находит лучший по формуле Эпли
     */
    private fun calculateBestSetsByDate(sets: List<SetWithDate>): List<WeightProgress> {
        return sets
            .groupBy { it.date }
            .map { (date, daySets) ->
                val bestSet = daySets.maxByOrNull { calculateEpley1RM(it.weight, it.reps) }!!
                val estimated1RM = calculateEpley1RM(bestSet.weight, bestSet.reps)
                val maxWeight = daySets.maxOf { it.weight }

                WeightProgress(
                    date = date,
                    maxWeight = maxWeight,
                    bestWeight = bestSet.weight,
                    bestReps = bestSet.reps,
                    estimated1RM = estimated1RM
                )
            }
            .sortedBy { it.date }
    }

    /**
     * Формула Эпли для расчёта 1RM (one-rep max)
     * 1RM = weight × (1 + reps/30)
     */
    private fun calculateEpley1RM(weight: Double, reps: Int): Double {
        if (reps <= 0) return weight
        return weight * (1.0 + reps.toDouble() / 30.0)
    }
    
    // ===== Full Workout Data =====
    
    /**
     * Получает полные данные тренировки включая упражнения и подходы
     */
    suspend fun getFullWorkout(workoutId: Long): FullWorkoutData? {
        val workout = getWorkoutById(workoutId) ?: return null
        val exercises = getWorkoutExercises(workoutId)
        
        val entries = exercises.map { we ->
            val exercise = getExerciseById(we.exerciseId)
            val sets = getSets(we.id)
            FullExerciseEntry(we, exercise, sets)
        }
        
        return FullWorkoutData(workout, entries)
    }
    
    /**
     * Сохраняет полную тренировку
     */
    suspend fun saveFullWorkout(
        date: String,
        entries: List<Pair<Long, List<ExerciseSet>>> // exerciseId -> sets
    ): Long {
        // Создаем или обновляем тренировку
        var workout = getWorkoutByDate(date)
        val workoutId = if (workout == null) {
            insertWorkout(Workout(date = date))
        } else {
            // Удаляем старые записи
            database.workoutExerciseDao().deleteByWorkout(workout.id)
            workout.id
        }
        
        // Добавляем упражнения и подходы
        entries.forEachIndexed { index, (exerciseId, sets) ->
            val weId = insertWorkoutExercise(
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    orderIndex = index
                )
            )
            
            sets.forEach { set ->
                insertSet(set.copy(workoutExerciseId = weId))
            }
        }
        
        return workoutId
    }
    
    // ===== Import / Export =====
    
    /**
     * Экспортирует все данные в JSON
     */
    suspend fun exportToJson(context: Context, uri: Uri) {
        val muscleGroups = getMuscleGroups()
        val exercises = getExercises()
        val workouts = getWorkouts()
        
        val workoutsJson = workouts.map { workout ->
            val entries = getWorkoutExercises(workout.id).map { we ->
                val exercise = getExerciseById(we.exerciseId)
                val sets = getSets(we.id)
                WorkoutEntryJson(
                    exercise = exercise?.name ?: "Unknown",
                    sets = sets.map { s ->
                        SetJson(s.setNumber, s.weight, s.reps, s.note)
                    }
                )
            }
            WorkoutJson(workout.date, entries)
        }
        
        val exercisesJson = exercises.map { ex ->
            val mg = ex.muscleGroupId?.let { getMuscleGroupById(it) }
            ExerciseJson(ex.name, mg?.name)
        }
        
        val exportData = ExportData(
            muscle_groups = muscleGroups.map { it.toJson() },
            exercises = exercisesJson,
            workouts = workoutsJson
        )
        
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                gson.toJson(exportData, writer)
            }
        }
    }
    
    /**
     * Импортирует данные из JSON
     */
    suspend fun importFromJson(context: Context, uri: Uri, clearExisting: Boolean = false) {
        val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).readText()
        } ?: throw Exception("Cannot read file")
        
        val importData = gson.fromJson(json, ExportData::class.java)
        
        if (clearExisting) {
            database.workoutDao().deleteAll()
            database.exerciseDao().deleteAll()
            database.muscleGroupDao().deleteAll()
            
            // Вставляем группы мышц
            importData.muscle_groups.forEach { mg ->
                database.muscleGroupDao().insert(mg.toEntity())
            }
        }
        
        // Кэш групп мышц по имени
        val muscleGroupsByName = getMuscleGroups().associateBy { it.name }
        
        // Импортируем упражнения
        importData.exercises.forEach { exJson ->
            val existing = getExerciseByName(exJson.name)
            if (existing == null) {
                val mgId = exJson.muscle_group?.let { muscleGroupsByName[it]?.id }
                insertExercise(Exercise(name = exJson.name, muscleGroupId = mgId))
            }
        }
        
        // Кэш упражнений по имени
        val exercisesByName = getExercises().associateBy { it.name }
        
        // Импортируем тренировки
        importData.workouts.forEach { workoutJson ->
            val existingWorkout = getWorkoutByDate(workoutJson.date)
            if (existingWorkout == null) {
                val workoutId = insertWorkout(Workout(date = workoutJson.date))
                
                workoutJson.entries.forEachIndexed { index, entry ->
                    val exerciseId = exercisesByName[entry.exercise]?.id
                    if (exerciseId != null) {
                        val weId = insertWorkoutExercise(
                            WorkoutExercise(
                                workoutId = workoutId,
                                exerciseId = exerciseId,
                                orderIndex = index
                            )
                        )
                        
                        entry.sets.forEach { setJson ->
                            insertSet(
                                ExerciseSet(
                                    workoutExerciseId = weId,
                                    setNumber = setJson.set_number,
                                    weight = setJson.weight,
                                    reps = setJson.reps,
                                    note = setJson.note
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// Вспомогательные классы

data class FullWorkoutData(
    val workout: Workout,
    val entries: List<FullExerciseEntry>
)

data class FullExerciseEntry(
    val workoutExercise: WorkoutExercise,
    val exercise: Exercise?,
    val sets: List<ExerciseSet>
)
