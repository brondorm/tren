package com.gymtracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymtracker.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MuscleGroup::class,
        Exercise::class,
        Workout::class,
        WorkoutExercise::class,
        ExerciseSet::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun muscleGroupDao(): MuscleGroupDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun exerciseSetDao(): ExerciseSetDao
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database)
                }
            }
        }

        private suspend fun populateDatabase(database: GymDatabase) {
            // Группы мышц
            val muscleGroups = listOf(
                MuscleGroup(1, "Грудь"),
                MuscleGroup(2, "Спина"),
                MuscleGroup(3, "Широчайшие", 2),
                MuscleGroup(4, "Трапеция", 2),
                MuscleGroup(5, "Плечи"),
                MuscleGroup(6, "Передняя дельта", 5),
                MuscleGroup(7, "Средняя дельта", 5),
                MuscleGroup(8, "Задняя дельта", 5),
                MuscleGroup(9, "Ноги"),
                MuscleGroup(10, "Квадрицепс", 9),
                MuscleGroup(11, "Бицепс бедра", 9),
                MuscleGroup(12, "Икры", 9),
                MuscleGroup(13, "Руки"),
                MuscleGroup(14, "Бицепс", 13),
                MuscleGroup(15, "Трицепс", 13),
                MuscleGroup(16, "Предплечья", 13),
            )
            database.muscleGroupDao().insertAll(muscleGroups)

            // Базовые упражнения
            val exercises = listOf(
                // Грудь
                Exercise(name = "Жим лежа", muscleGroupId = 1),
                Exercise(name = "Жим в наклоне", muscleGroupId = 1),
                Exercise(name = "Жим гантелей", muscleGroupId = 1),
                Exercise(name = "Брусья", muscleGroupId = 1),
                
                // Спина / Широчайшие
                Exercise(name = "Подтягивания", muscleGroupId = 3),
                Exercise(name = "Тяга вертикальная", muscleGroupId = 3),
                Exercise(name = "Тяга горизонтальная", muscleGroupId = 3),
                Exercise(name = "Тяга гантели", muscleGroupId = 3),
                
                // Плечи
                Exercise(name = "Махи в стороны", muscleGroupId = 7),
                Exercise(name = "Жим стоя", muscleGroupId = 6),
                Exercise(name = "Разводка назад", muscleGroupId = 8),
                
                // Ноги
                Exercise(name = "Присед", muscleGroupId = 10),
                Exercise(name = "Жим ногами", muscleGroupId = 10),
                Exercise(name = "Разгибания ног", muscleGroupId = 10),
                Exercise(name = "Сгибания ног", muscleGroupId = 11),
                Exercise(name = "Румынская тяга", muscleGroupId = 11),
                Exercise(name = "Подъем на носки", muscleGroupId = 12),
                
                // Бицепс
                Exercise(name = "Подъем на бицепс", muscleGroupId = 14),
                Exercise(name = "Молотки", muscleGroupId = 14),
                
                // Трицепс
                Exercise(name = "Французский жим", muscleGroupId = 15),
                Exercise(name = "Разгибания на трицепс", muscleGroupId = 15),
            )
            database.exerciseDao().insertAll(exercises)
        }
    }
}
