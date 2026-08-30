package com.jesunez.recetairo.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.ui.HomeUiState
import com.jesunez.recetairo.ui.component.BottomNavigationBar
import com.jesunez.recetairo.ui.component.CategoryGrid
import com.jesunez.recetairo.ui.component.ExpiringSoonSection
import com.jesunez.recetairo.ui.component.HomeHeader
import com.jesunez.recetairo.ui.component.QuickActionsSection
import com.jesunez.recetairo.ui.theme.RecetairoTheme
import com.jesunez.recetairo.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onAddManually: () -> Unit,
    onScanBarcode: () -> Unit,
    onScanReceipt: () -> Unit,
    onCategoryClick: (FoodCategory) -> Unit,
    onViewAllExpiringClick: () -> Unit,
    onNavigateToPantry: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onFoodClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeContent(
        uiState = uiState,
        onAddManually = onAddManually,
        onScanBarcode = onScanBarcode,
        onScanReceipt = onScanReceipt,
        onCategoryClick = onCategoryClick,
        onViewAllExpiringClick = onViewAllExpiringClick,
        onNavigateToPantry = onNavigateToPantry,
        onNavigateToRecipes = onNavigateToRecipes,
        onFoodClick = onFoodClick,
        modifier = modifier
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onAddManually: () -> Unit,
    onScanBarcode: () -> Unit,
    onScanReceipt: () -> Unit,
    onCategoryClick: (FoodCategory) -> Unit,
    onViewAllExpiringClick: () -> Unit,
    onNavigateToPantry: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onFoodClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeHeader(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "home",
                onItemClick = { route ->
                    // R30: the "Recetas" tab, previously without effect, now navigates to Menu_Recetas
                    when (route) {
                        "pantry" -> onNavigateToPantry()
                        "recipes" -> onNavigateToRecipes()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // R14: the section is omitted entirely when there is nothing expiring soon, not
            // shown with an empty state.
            if (uiState.expiringSoonFoods.isNotEmpty()) {
                ExpiringSoonSection(
                    items = uiState.expiringSoonFoods,
                    onViewAllClick = onViewAllExpiringClick,
                    onItemClick = onFoodClick
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            QuickActionsSection(
                onScanReceipt = onScanReceipt,
                onScanBarcode = onScanBarcode,
                onAddManually = onAddManually
            )

            Spacer(modifier = Modifier.height(24.dp))

            CategoryGrid(
                categories = uiState.categorySummaries,
                onCategoryClick = onCategoryClick
            )

            // Padding to ensure content is not hidden behind the floating bottom bar
            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
fun HomeScreenPreview() {
    RecetairoTheme {
        HomeContent(
            uiState = HomeUiState(isLoading = false),
            onAddManually = {},
            onScanBarcode = {},
            onScanReceipt = {},
            onCategoryClick = {},
            onViewAllExpiringClick = {},
            onNavigateToPantry = {},
            onNavigateToRecipes = {},
            onFoodClick = {}
        )
    }
}
