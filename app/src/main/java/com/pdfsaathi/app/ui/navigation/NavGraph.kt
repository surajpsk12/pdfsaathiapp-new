package com.pdfsaathi.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pdfsaathi.app.ui.documents.AllDocumentsScreen
import com.pdfsaathi.app.ui.favorites.FavoritesScreen
import com.pdfsaathi.app.ui.home.HomeScreen
import com.pdfsaathi.app.ui.reader.PdfReaderScreen
import com.pdfsaathi.app.ui.search.SearchScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Documents : Screen("documents", "Files", Icons.Default.Folder)
    object Favorites : Screen("favorites", "Favorites", Icons.Default.Star)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Reader : Screen("reader/{documentId}", "Reader") {
        fun createRoute(documentId: String) = "reader/${Uri.encode(documentId)}"
    }
}

@Composable
fun PdfNavGraph(
    navController: NavHostController = rememberNavController(),
    externalPdfUri: Uri? = null
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Documents,
        Screen.Favorites,
        Screen.Search
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    LaunchedEffect(externalPdfUri) {
        externalPdfUri?.let { uri ->
            navController.navigate(Screen.Reader.createRoute(uri.toString()))
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { item.icon?.let { Icon(it, contentDescription = item.title) } },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onOpenReader = { doc -> navController.navigate(Screen.Reader.createRoute(doc.id)) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) }
                )
            }
            composable(Screen.Documents.route) {
                AllDocumentsScreen(
                    onOpenReader = { doc -> navController.navigate(Screen.Reader.createRoute(doc.id)) }
                )
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onOpenReader = { doc -> navController.navigate(Screen.Reader.createRoute(doc.id)) }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onOpenReader = { doc -> navController.navigate(Screen.Reader.createRoute(doc.id)) }
                )
            }
            composable(
                route = Screen.Reader.route,
                arguments = listOf(navArgument("documentId") { type = NavType.StringType })
            ) { backStackEntry ->
                val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
                PdfReaderScreen(
                    documentId = documentId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
