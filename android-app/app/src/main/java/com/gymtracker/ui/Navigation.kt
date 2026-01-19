package com.gymtracker.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gymtracker.data.repository.GymRepository
import com.gymtracker.ui.screens.*

// Общие настройки анимаций
private const val ANIM_DURATION = 350

private val enterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    fadeIn(
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
    ) + slideInHorizontally(
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing),
        initialOffsetX = { it / 4 }
    )
}

private val exitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    fadeOut(
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
    ) + slideOutHorizontally(
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing),
        targetOffsetX = { -it / 4 }
    )
}

private val popEnterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    fadeIn(
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
    ) + slideInHorizontally(
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing),
        initialOffsetX = { -it / 4 }
    )
}

private val popExitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    fadeOut(
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
    ) + slideOutHorizontally(
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing),
        targetOffsetX = { it / 4 }
    )
}

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
        composable(
            route = Screen.Workouts.route,
            enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) }
        ) {
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
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: return@composable
            EditWorkoutScreen(
                repository = repository,
                date = date,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Справочник упражнений
        composable(
            route = Screen.Exercises.route,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition
        ) {
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
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType }),
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: 0L
            EditExerciseScreen(
                repository = repository,
                exerciseId = exerciseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Статистика по мышцам
        composable(
            route = Screen.Stats.route,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition
        ) {
            StatsScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Прогресс весов
        composable(
            route = Screen.Progress.route,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition
        ) {
            ProgressScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Настройки (импорт/экспорт)
        composable(
            route = Screen.Settings.route,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition
        ) {
            SettingsScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
