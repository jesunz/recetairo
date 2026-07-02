// Feature: insertar-alimentos-despensa, Property 8
package com.jesunez.recetairo.feature.food.ui.viewmodel

import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import com.jesunez.recetairo.feature.food.domain.repository.FoodRepository
import com.jesunez.recetairo.feature.food.domain.usecase.InsertFoodUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.SearchFoodHistoryUseCase
import com.jesunez.recetairo.feature.food.domain.usecase.ValidateFoodUseCase
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private fun Arb.Companion.productInfo(): Arb<ProductInfo> = Arb.bind(
    Arb.string(1..20),
    Arb.string(1..50).orNull(),
    Arb.string(1..50).orNull(),
    Arb.string(1..50).orNull(),
    Arb.string(1..100).orNull()
) { barcode, name, brand, category, imageUrl ->
    ProductInfo(barcode = barcode, name = name, brand = brand, category = category, imageUrl = imageUrl)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddFoodViewModelPropertyTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): AddFoodViewModel {
        val fakeRepository = object : FoodRepository {
            override suspend fun insertFood(food: Food): Result<Unit> = Result.Success(Unit)

            override suspend fun insertFoods(foods: List<Food>): Result<Unit> = Result.Success(Unit)

            override fun searchFoodNames(query: String): Flow<List<String>> = flowOf(emptyList())
        }
        return AddFoodViewModel(
            validateFoodUseCase = ValidateFoodUseCase(),
            insertFoodUseCase = InsertFoodUseCase(fakeRepository),
            searchFoodHistoryUseCase = SearchFoodHistoryUseCase(fakeRepository)
        )
    }

    @Test
    fun should_prefillOnlyReceivedFields_when_productInfoIsApplied() = runTest(testDispatcher) {
        // R15: cada campo con dato recibido se pre-rellena; los nulos dejan el campo sin modificar
        checkAll(100, Arb.productInfo()) { productInfo ->
            val viewModel = buildViewModel()

            viewModel.onProductInfoReceived(productInfo)
            val state = viewModel.uiState.value

            assertEquals(
                "R15: el nombre debe pre-rellenarse con el dato recibido",
                productInfo.name ?: "",
                state.name
            )
            assertEquals(
                "R15: la marca debe pre-rellenarse con el dato recibido",
                productInfo.brand ?: "",
                state.brand
            )
            assertEquals(
                "R15: la categoría debe pre-rellenarse con el dato recibido",
                productInfo.category ?: "",
                state.category
            )
            assertEquals(
                "R15: la imagen debe pre-rellenarse con el dato recibido",
                productInfo.imageUrl,
                state.imageUrl
            )
        }
    }

    @Test
    fun should_leaveFieldsEditable_when_userModifiesAfterPrefill() = runTest(testDispatcher) {
        // R15: el usuario debe poder revisar y modificar los campos pre-rellenados
        checkAll(100, Arb.productInfo(), Arb.string(1..30)) { productInfo, editedName ->
            val viewModel = buildViewModel()

            viewModel.onProductInfoReceived(productInfo)
            viewModel.onNameChanged(editedName)

            assertEquals(
                "R15: el nombre editado por el usuario debe prevalecer sobre el pre-relleno",
                editedName,
                viewModel.uiState.value.name
            )
        }
    }
}
