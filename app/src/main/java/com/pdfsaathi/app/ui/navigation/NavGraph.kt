package com.pdfsaathi.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pdfsaathi.app.ui.developer.DeveloperScreen
import com.pdfsaathi.app.ui.documents.AllDocumentsScreen
import com.pdfsaathi.app.ui.favorites.FavoritesScreen
import com.pdfsaathi.app.ui.home.HomeScreen
import com.pdfsaathi.app.ui.reader.PdfReaderScreen
import com.pdfsaathi.app.ui.search.SearchScreen
import com.pdfsaathi.app.ui.settings.SettingsScreen
import com.pdfsaathi.app.ui.splash.SplashScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Splash : Screen("splash", "Splash")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Documents : Screen("documents", "Files", Icons.Default.Folder)
    object Favorites : Screen("favorites", "Favorites", Icons.Default.Star)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Developer : Screen("developer", "Developer", Icons.Default.Person)
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
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Settings,
        Screen.Developer
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    val selectedRoute = when (currentRoute) {
        Screen.Home.route -> Screen.Home.route
        Screen.Settings.route -> Screen.Settings.route
        Screen.Developer.route -> Screen.Developer.route
        else -> null
    }

    LaunchedEffect(externalPdfUri) {
        externalPdfUri?.let { uri ->
            navController.navigate(Screen.Reader.createRoute(uri.toString()))
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = selectedRoute == item.route
                        NavigationBarItem(
                            icon = { item.icon?.let { Icon(it, contentDescription = item.title) } },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                if (selectedRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onSplashFinished = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Home.route) {
                    HomeScreen(
                        onOpenReader = { doc -> navController.navigate(Screen.Reader.createRoute(doc.id)) },
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) }
                    )
                }
                composable(Screen.Documents.route) {
                    AllDocumentsScreen(
                        onOpenReader = { doc -> navController.navigate(Screen.Reader.createRoute(doc.id)) },
                        onBackClick = { navController.navigate(Screen.Home.route) }
                    )
                }
                composable(Screen.Favorites.route) {
                    FavoritesScreen(
                        onOpenReader = { doc -> navController.navigate(Screen.Reader.createRoute(doc.id)) }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
                composable(Screen.Developer.route) {
                    DeveloperScreen()
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
}
