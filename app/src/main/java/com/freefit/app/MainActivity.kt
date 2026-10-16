package com.freefit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.freefit.app.data.ExerciseLibrary
import com.freefit.app.data.Plans
import com.freefit.app.fitness.PlayerScreen
import com.freefit.app.fitness.adhocPlan
import com.freefit.app.screens.ExerciseDetailScreen
import com.freefit.app.screens.HistoryScreen
import com.freefit.app.screens.HomeScreen
import com.freefit.app.screens.LibraryScreen
import com.freefit.app.ui.FreeFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FreeFitTheme {
                FreeFitApp()
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun FreeFitApp() {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", "首页", Icons.Filled.Home),
        Tab("library", "动作库", Icons.Filled.FitnessCenter),
        Tab("history", "记录", Icons.Filled.History),
    )
    val tabRoutes = tabs.map { it.route }.toSet()

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(onStartPlan = { nav.navigate("player/plan/$it") })
            }
            composable("library") {
                LibraryScreen(onOpenExercise = { nav.navigate("exercise/$it") })
            }
            composable("history") {
                HistoryScreen()
            }
            composable("exercise/{id}") { entry ->
                val ex = ExerciseLibrary.byId(entry.arguments?.getString("id") ?: "squat")
                ExerciseDetailScreen(
                    ex = ex,
                    onBack = { nav.popBackStack() },
                    onTry = { nav.navigate("player/exercise/${ex.id}") },
                )
            }
            composable("player/plan/{id}") { entry ->
                val plan = Plans.byId(entry.arguments?.getString("id") ?: "p1")
                PlayerScreen(plan = plan, onExit = { nav.popBackStack() })
            }
            composable("player/exercise/{id}") { entry ->
                val ex = ExerciseLibrary.byId(entry.arguments?.getString("id") ?: "squat")
                PlayerScreen(plan = adhocPlan(ex), onExit = { nav.popBackStack() })
            }
        }
    }
}
