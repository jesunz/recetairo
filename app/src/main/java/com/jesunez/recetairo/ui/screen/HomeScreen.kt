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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jesunez.recetairo.ui.component.BottomNavigationBar
import com.jesunez.recetairo.ui.component.CategoryGrid
import com.jesunez.recetairo.ui.component.ExpiringSoonSection
import com.jesunez.recetairo.ui.component.HomeHeader
import com.jesunez.recetairo.ui.component.QuickActionsSection
import com.jesunez.recetairo.ui.theme.RecetairoTheme

@Composable
fun HomeScreen(
    onAddManually: () -> Unit,
    onScanBarcode: () -> Unit,
    onScanReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeHeader(
                modifier = Modifier.padding(horizontal = 16.dp),
                onNotificationClick = { /* TODO: Implement notifications */ },
                onProfileClick = { /* TODO: Implement profile */ }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "home",
                onItemClick = { /* TODO: Implement navigation */ }
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
            
            ExpiringSoonSection(
                onViewAllClick = { /* TODO: View all expiring items */ }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            QuickActionsSection(
                onScanReceipt = onScanReceipt,
                onScanBarcode = onScanBarcode,
                onAddManually = onAddManually
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            CategoryGrid(
                onCategoryClick = { /* TODO: Navigate to category */ }
            )
            
            // Padding to ensure content is not hidden behind the floating bottom bar
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
fun HomeScreenPreview() {
    RecetairoTheme {
        HomeScreen(
            onAddManually = {},
            onScanBarcode = {},
            onScanReceipt = {}
        )
    }
}
