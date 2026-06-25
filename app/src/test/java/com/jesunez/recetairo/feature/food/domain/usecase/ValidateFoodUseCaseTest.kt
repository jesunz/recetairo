package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ValidateFoodUseCaseTest {

    private lateinit var useCase: ValidateFoodUseCase

    @Before
    fun setUp() {
        useCase = ValidateFoodUseCase()
    }

    // region name

    @Test
    fun should_returnNameError_when_nameIsBlank() {
        // Given
        val food = validFood().copy(name = "   ")
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(FoodField.NAME))
    }

    @Test
    fun should_returnNameError_when_nameIsEmpty() {
        // Given
        val food = validFood().copy(name = "")
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(FoodField.NAME))
    }

    @Test
    fun should_returnNameError_when_nameExceeds100Chars() {
        // Given
        val food = validFood().copy(name = "a".repeat(101))
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(FoodField.NAME))
    }

    @Test
    fun should_returnValid_when_nameIsExactly100Chars() {
        // Given
        val food = validFood().copy(name = "a".repeat(100))
        // When
        val result = useCase(food)
        // Then
        assertTrue(result.isValid)
        assertFalse(result.errors.containsKey(FoodField.NAME))
    }

    // endregion

    // region quantity

    @Test
    fun should_returnQuantityError_when_quantityIsBelowMinimum() {
        // Given
        val food = validFood().copy(quantity = 0.0)
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(FoodField.QUANTITY))
    }

    @Test
    fun should_returnQuantityError_when_quantityIsNegative() {
        // Given
        val food = validFood().copy(quantity = -1.0)
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(FoodField.QUANTITY))
    }

    @Test
    fun should_returnQuantityError_when_quantityExceedsMaximum() {
        // Given
        val food = validFood().copy(quantity = 100.0)
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(FoodField.QUANTITY))
    }

    @Test
    fun should_returnValid_when_quantityIsAtMinimumBoundary() {
        // Given
        val food = validFood().copy(quantity = ValidateFoodUseCase.MIN_QUANTITY)
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.errors.containsKey(FoodField.QUANTITY))
    }

    @Test
    fun should_returnValid_when_quantityIsAtMaximumBoundary() {
        // Given
        val food = validFood().copy(quantity = ValidateFoodUseCase.MAX_QUANTITY)
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.errors.containsKey(FoodField.QUANTITY))
    }

    // endregion

    // region expiryDate

    @Test
    fun should_returnExpiryDateError_when_expiryDateIsYesterday() {
        // Given
        val food = validFood().copy(expiryDate = LocalDate.now().minusDays(1))
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.containsKey(FoodField.EXPIRY_DATE))
    }

    @Test
    fun should_returnValid_when_expiryDateIsToday() {
        // Given
        val food = validFood().copy(expiryDate = LocalDate.now())
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.errors.containsKey(FoodField.EXPIRY_DATE))
    }

    @Test
    fun should_returnValid_when_expiryDateIsInTheFuture() {
        // Given
        val food = validFood().copy(expiryDate = LocalDate.now().plusDays(30))
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.errors.containsKey(FoodField.EXPIRY_DATE))
    }

    @Test
    fun should_returnValid_when_expiryDateIsNull() {
        // Given
        val food = validFood().copy(expiryDate = null)
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.errors.containsKey(FoodField.EXPIRY_DATE))
    }

    // endregion

    // region combined

    @Test
    fun should_returnValid_when_allFieldsAreValid() {
        // Given
        val food = validFood()
        // When
        val result = useCase(food)
        // Then
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun should_returnMultipleErrors_when_nameAndQuantityAreInvalid() {
        // Given
        val food = validFood().copy(name = "", quantity = 0.0)
        // When
        val result = useCase(food)
        // Then
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertTrue(result.errors.containsKey(FoodField.NAME))
        assertTrue(result.errors.containsKey(FoodField.QUANTITY))
    }

    // endregion

    private fun validFood() = Food(
        name = "Leche",
        quantity = 1.0,
        expiryDate = LocalDate.now().plusDays(7)
    )
}
