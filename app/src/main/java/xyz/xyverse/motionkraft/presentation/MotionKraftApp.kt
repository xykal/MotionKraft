package xyz.xyverse.motionkraft.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.xyverse.motionkraft.data.ProjectStore
import xyz.xyverse.motionkraft.presentation.screens.EditorScreen
import xyz.xyverse.motionkraft.presentation.screens.HomeScreen

@Composable
fun MotionKraftRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val store = remember { ProjectStore(LocalContext.current) }
    var refresh by remember { mutableStateOf(0) }
    val vm: EditorViewModel = viewModel()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            val projects = remember(backStack, refresh) { store.list() }
            HomeScreen(
                projects = projects,
                onCreate = { width, height ->
                    val id = vm.createProject(width, height)
                    nav.navigate("editor/$id")
                },
                onOpen = { id -> nav.navigate("editor/$id") },
                onDelete = { id ->
                    store.delete(id)
                    refresh++
                }
            )
        }
        composable(
            route = "editor/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            EditorScreen(vm = vm, projectId = id, onBack = { nav.popBackStack() })
        }
    }
}
