package com.gymtracker.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gymtracker.data.repository.GymRepository
import com.gymtracker.ui.screens.*

sealed class Screen(val route: String) {
    object Workouts : Screen("workouts")
    object EditWorkout : Screen("edit_workout/{date}") {
        fun createRoute(date: String) = "edit_workout/$date"
    }
    object Exercises : Screen("exercises")
    object EditExercise : Screen("edit_exercise/{exerciseId}") {
        fun createRoute(exerciseId: Long) = "edit_exercise/$exerciseId"
    }
    object Stats : Screen("stats")
    object Progress : Screen("progress")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    repository: GymRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Workouts.route
    ) {
        // Список тренировок
        composable(Screen.Workouts.route) {
            WorkoutsScreen(
                repository = repository,
                onNavigateToWorkout = { date ->
                    navController.navigate(Screen.EditWorkout.createRoute(date))
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onNavigateToProgress = {
                    navController.navigate(Screen.Progress.route)
                },
                onNavigateToExercises = {
                    navController.navigate(Screen.Exercises.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        // Редактирование тренировки
        composable(
            route = Screen.EditWorkout.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: return@composable
            EditWorkoutScreen(
                repository = repository,
                date = date,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Справочник упражнений
        composable(Screen.Exercises.route) {
            ExercisesScreen(
                repository = repository,
                onNavigateToEdit = { exerciseId ->
                    navController.navigate(Screen.EditExercise.createRoute(exerciseId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Редактирование упражнения
        composable(
            route = Screen.EditExercise.route,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: 0L
            EditExerciseScreen(
                repository = repository,
                exerciseId = exerciseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Статистика по мышцам
        composable(Screen.Stats.route) {
            StatsScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Прогресс весов
        composable(Screen.Progress.route) {
            ProgressScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Настройки (импорт/экспорт)
        composable(Screen.Settings.route) {
            SettingsScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
