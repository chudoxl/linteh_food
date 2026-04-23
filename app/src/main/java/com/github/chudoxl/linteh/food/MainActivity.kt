package com.github.chudoxl.linteh.food

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.chudoxl.linteh.food.screens.about.AboutScreen
import com.github.chudoxl.linteh.food.screens.about.LicensesScreen
import com.github.chudoxl.linteh.food.screens.edit.EditScreen
import com.github.chudoxl.linteh.food.screens.main.MainScreen
import com.github.chudoxl.linteh.food.ui.theme.AppTheme
import com.github.chudoxl.linteh.food.worker.NotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.cancelAll(this)
        setContent {
            AppTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "main",
                    enterTransition = { slideIntoContainer(SlideDirection.Start, tween(300)) },
                    exitTransition = { slideOutOfContainer(SlideDirection.Start, tween(300)) },
                    popEnterTransition = { slideIntoContainer(SlideDirection.End, tween(300)) },
                    popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(300)) },
                ) {
                    composable("main") {
                        MainScreen(
                            onAddClick = { navController.navigate("edit") },
                            onEditClick = { cardNumber -> navController.navigate("edit?cardNumber=$cardNumber") },
                            onAboutClick = { navController.navigate("about") },
                        )
                    }
                    composable("about") {
                        AboutScreen(
                            onBack = { navController.popBackStack() },
                            onLicensesClick = { navController.navigate("licenses") },
                        )
                    }
                    composable("licenses") {
                        LicensesScreen(onBack = { navController.popBackStack() })
                    }
                    composable(
                        route = "edit?cardNumber={cardNumber}",
                        arguments = listOf(navArgument("cardNumber") {
                            type = NavType.StringType
                            defaultValue = ""
                        })
                    ) { backStackEntry ->
                        val cardNumber = backStackEntry.arguments?.getString("cardNumber")
                        EditScreen(
                            cardNumber = cardNumber,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotificationHelper.cancelAll(this)
    }
}
