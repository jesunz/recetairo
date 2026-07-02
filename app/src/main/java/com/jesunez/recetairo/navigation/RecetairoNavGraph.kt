package com.jesunez.recetairo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import com.jesunez.recetairo.feature.food.ui.screen.AddFoodScreen
import com.jesunez.recetairo.feature.food.ui.screen.BarcodeScanScreen
import com.jesunez.recetairo.feature.food.ui.screen.ReceiptScanScreen
import com.jesunez.recetairo.ui.screen.HomeScreen

@Composable
fun RecetairoNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAddManually = { navController.navigate(Screen.AddFood.buildRoute()) },
                onScanBarcode = { navController.navigate(Screen.BarcodeScan.route) },
                onScanReceipt = { navController.navigate(Screen.ReceiptScan.route) }
            )
        }

        composable(Screen.BarcodeScan.route) {
            BarcodeScanScreen(
                onNavigateToAddFood = { productInfo ->
                    navController.navigate(Screen.AddFood.buildRoute(productInfo)) {
                        popUpTo(Screen.BarcodeScan.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ReceiptScan.route) {
            ReceiptScanScreen(
                onNavigateToAddFood = {
                    navController.navigate(Screen.AddFood.buildRoute()) {
                        popUpTo(Screen.ReceiptScan.route) { inclusive = true }
                    }
                },
                onFinish = {
                    navController.popBackStack(Screen.Home.route, false)
                }
            )
        }

        composable(
            route = Screen.AddFood.route,
            arguments = listOf(
                navArgument("barcode") { type = NavType.StringType; nullable = true },
                navArgument("name") { type = NavType.StringType; nullable = true },
                navArgument("brand") { type = NavType.StringType; nullable = true },
                navArgument("category") { type = NavType.StringType; nullable = true },
                navArgument("imageUrl") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode")
            val productInfo = barcode?.let {
                ProductInfo(
                    barcode = it,
                    name = backStackEntry.arguments?.getString("name"),
                    brand = backStackEntry.arguments?.getString("brand"),
                    category = backStackEntry.arguments?.getString("category"),
                    imageUrl = backStackEntry.arguments?.getString("imageUrl")
                )
            }
            AddFoodScreen(
                onNavigateBack = { navController.popBackStack() },
                productInfo = productInfo
            )
        }
    }
}
