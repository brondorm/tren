package com.gymtracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
    version = 3,
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

        /**
         * Миграция с версии 1 на 2: добавляем поле synergistMuscleGroupId
         * и автоматически проставляем синергисты для стандартных упражнений
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Добавляем колонку synergistMuscleGroupId
                db.execSQL("ALTER TABLE exercises ADD COLUMN synergistMuscleGroupId INTEGER DEFAULT NULL")

                // Создаём индекс для новой колонки
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercises_synergistMuscleGroupId ON exercises(synergistMuscleGroupId)")

                // Автоматически проставляем синергисты для известных упражнений
                // Грудь -> Трицепс (15)
                db.execSQL("UPDATE exercises SET synergistMuscleGroupId = 15 WHERE name IN ('Жим лежа', 'Жим в наклоне', 'Жим гантелей', 'Брусья')")

                // Спина/Широчайшие -> Бицепс (14)
                db.execSQL("UPDATE exercises SET synergistMuscleGroupId = 14 WHERE name IN ('Подтягивания', 'Тяга вертикальная', 'Тяга горизонтальная', 'Тяга гантели')")

                // Жим стоя (плечи) -> Трицепс (15)
                db.execSQL("UPDATE exercises SET synergistMuscleGroupId = 15 WHERE name = 'Жим стоя'")

                // Присед и Жим ногами -> Бицепс бедра (11)
                db.execSQL("UPDATE exercises SET synergistMuscleGroupId = 11 WHERE name IN ('Присед', 'Жим ногами')")

                // Румынская тяга -> Квадрицепс (10)
                db.execSQL("UPDATE exercises SET synergistMuscleGroupId = 10 WHERE name = 'Румынская тяга'")
            }
        }

        /**
         * Миграция с версии 2 на 3: добавляем второй синергист synergistMuscleGroupId2
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Добавляем колонку synergistMuscleGroupId2
                db.execSQL("ALTER TABLE exercises ADD COLUMN synergistMuscleGroupId2 INTEGER DEFAULT NULL")

                // Создаём индекс для новой колонки
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercises_synergistMuscleGroupId2 ON exercises(synergistMuscleGroupId2)")
            }
        }

        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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

            // Базовые упражнения с синергистами
            // Трицепс = 15, Бицепс = 14, Квадрицепс = 10, Бицепс бедра = 11
            val exercises = listOf(
                // Грудь -> синергист Трицепс
                Exercise(name = "Жим лежа", muscleGroupId = 1, synergistMuscleGroupId = 15),
                Exercise(name = "Жим в наклоне", muscleGroupId = 1, synergistMuscleGroupId = 15),
                Exercise(name = "Жим гантелей", muscleGroupId = 1, synergistMuscleGroupId = 15),
                Exercise(name = "Брусья", muscleGroupId = 1, synergistMuscleGroupId = 15),

                // Спина / Широчайшие -> синергист Бицепс
                Exercise(name = "Подтягивания", muscleGroupId = 3, synergistMuscleGroupId = 14),
                Exercise(name = "Тяга вертикальная", muscleGroupId = 3, synergistMuscleGroupId = 14),
                Exercise(name = "Тяга горизонтальная", muscleGroupId = 3, synergistMuscleGroupId = 14),
                Exercise(name = "Тяга гантели", muscleGroupId = 3, synergistMuscleGroupId = 14),

                // Плечи
                Exercise(name = "Махи в стороны", muscleGroupId = 7), // изоляция, без синергиста
                Exercise(name = "Жим стоя", muscleGroupId = 6, synergistMuscleGroupId = 15), // -> Трицепс
                Exercise(name = "Разводка назад", muscleGroupId = 8), // изоляция, без синергиста

                // Ноги
                Exercise(name = "Присед", muscleGroupId = 10, synergistMuscleGroupId = 11), // Квадр -> Бицепс бедра
                Exercise(name = "Жим ногами", muscleGroupId = 10, synergistMuscleGroupId = 11),
                Exercise(name = "Разгибания ног", muscleGroupId = 10), // изоляция
                Exercise(name = "Сгибания ног", muscleGroupId = 11), // изоляция
                Exercise(name = "Румынская тяга", muscleGroupId = 11, synergistMuscleGroupId = 10), // Бицепс бедра -> Квадр
                Exercise(name = "Подъем на носки", muscleGroupId = 12), // изоляция

                // Бицепс - изоляция
                Exercise(name = "Подъем на бицепс", muscleGroupId = 14),
                Exercise(name = "Молотки", muscleGroupId = 14),

                // Трицепс - изоляция
                Exercise(name = "Французский жим", muscleGroupId = 15),
                Exercise(name = "Разгибания на трицепс", muscleGroupId = 15),
            )
            database.exerciseDao().insertAll(exercises)
        }
    }
}
