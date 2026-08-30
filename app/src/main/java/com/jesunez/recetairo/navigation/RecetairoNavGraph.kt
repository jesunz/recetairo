package com.jesunez.recetairo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jesunez.recetairo.feature.food.domain.model.PantryFilter
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import com.jesunez.recetairo.feature.food.ui.screen.AddFoodScreen
import com.jesunez.recetairo.feature.food.ui.screen.BarcodeScanScreen
import com.jesunez.recetairo.feature.food.ui.screen.FoodDetailScreen
import com.jesunez.recetairo.feature.food.ui.screen.PantryScreen
import com.jesunez.recetairo.feature.food.ui.screen.ReceiptScanScreen
import com.jesunez.recetairo.feature.recipe.ui.screen.GeneratedRecipesScreen
import com.jesunez.recetairo.feature.recipe.ui.screen.IngredientSelectionScreen
import com.jesunez.recetairo.feature.recipe.ui.screen.RecipeDetailScreen
import com.jesunez.recetairo.feature.recipe.ui.screen.RecipeListScreen
import com.jesunez.recetairo.ui.screen.HomeScreen

@Composable
fun RecetairoNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAddManually = { navController.navigate(Screen.AddFood.buildRoute()) },
                onScanBarcode = { navController.navigate(Screen.BarcodeScan.route) },
                onScanReceipt = { navController.navigate(Screen.ReceiptScan.route) },
                onCategoryClick = { category ->
                    navController.navigate(Screen.Pantry.buildRoute(PantryFilter.ByCategory(category)))
                },
                onViewAllExpiringClick = {
                    navController.navigate(Screen.Pantry.buildRoute(PantryFilter.ExpiringSoon))
                },
                onNavigateToPantry = {
                    navController.navigate(Screen.Pantry.buildRoute(PantryFilter.All))
                },
                onNavigateToRecipes = {
                    navController.navigate(Screen.RecipeList.route)
                },
                onFoodClick = { foodId ->
                    navController.navigate(Screen.FoodDetail.buildRoute(foodId))
                }
            )
        }

        composable(Screen.RecipeList.route) {
            RecipeListScreen(
                onGenerateClick = {
                    navController.navigate(Screen.IngredientSelection.route)
                },
                onRecipeClick = { recipeId ->
                    navController.navigate(Screen.RecipeDetail.buildRoute(recipeId))
                }
            )
        }

        composable(Screen.IngredientSelection.route) {
            IngredientSelectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onGenerateRecipeClick = { ingredientNames, _ ->
                    // T4 de seleccion-raciones-recetas: `servings` todavía no viaja por la ruta;
                    // T5 extiende Screen.GeneratedRecipes para incluirlo.
                    navController.navigate(Screen.GeneratedRecipes.buildRoute(ingredientNames))
                }
            )
        }

        composable(
            route = Screen.GeneratedRecipes.route,
            arguments = listOf(navArgument("ingredientNames") { type = NavType.StringType })
        ) {
            GeneratedRecipesScreen(
                onNavigateBack = { navController.popBackStack() },
                onRecipeClick = { generatedIndex ->
                    // R18: navega a Detalle_Receta con generatedIndex — la Receta aún no está
                    // guardada, RecipeDetailViewModel la resuelve vía GeneratedRecipesCache
                    navController.navigate(Screen.RecipeDetail.buildRoute(generatedIndex))
                },
                onSavedSuccessfully = {
                    // R20: tras guardar, vuelta a Menu_Recetas — descarta Selector_Ingredientes y
                    // Recetas_Generadas de la pila
                    navController.popBackStack(Screen.RecipeList.route, false)
                }
            )
        }

        composable(
            route = Screen.RecipeDetail.route,
            arguments = listOf(
                // NavType.LongType/IntType no admite nullable = true en Navigation Compose;
                // mismo patrón StringType nullable que Screen.Pantry, parseado a mano en el
                // ViewModel (mismo criterio que PantryViewModel.toPantryFilter())
                navArgument("recipeId") { type = NavType.StringType; nullable = true },
                navArgument("generatedIndex") { type = NavType.StringType; nullable = true }
            )
        ) {
            RecipeDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Pantry.route,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType; nullable = true },
                navArgument("expiringSoon") { type = NavType.StringType; nullable = true }
            )
        ) {
            PantryScreen(
                onNavigateBack = { navController.popBackStack() },
                onFoodClick = { foodId ->
                    navController.navigate(Screen.FoodDetail.buildRoute(foodId))
                }
            )
        }

        composable(
            route = Screen.FoodDetail.route,
            arguments = listOf(navArgument("foodId") { type = NavType.LongType })
        ) {
            FoodDetailScreen(
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateBack = { navController.popBackStack() },
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
