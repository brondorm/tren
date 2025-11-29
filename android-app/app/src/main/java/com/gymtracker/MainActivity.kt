package com.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.gymtracker.data.database.GymDatabase
import com.gymtracker.data.repository.GymRepository
import com.gymtracker.ui.AppNavigation
import com.gymtracker.ui.theme.GymTrackerTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var repository: GymRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализация базы данных и репозитория
        val database = GymDatabase.getDatabase(applicationContext)
        repository = GymRepository(database)
        
        setContent {
            GymTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        repository = repository
                    )
                }
            }
        }
    }
}
