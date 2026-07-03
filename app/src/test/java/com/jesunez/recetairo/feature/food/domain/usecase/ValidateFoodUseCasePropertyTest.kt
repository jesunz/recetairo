// Feature: insertar-alimentos-despensa, Property 2
package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodField
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ValidateFoodUseCasePropertyTest {

    private lateinit var useCase: ValidateFoodUseCase

    @Before
    fun setUp() {
        useCase = ValidateFoodUseCase()
    }

    @Test
    fun should_flagNameError_when_nameIsBlankOrExceedsMaxLength() {
        runBlocking {
            checkAll(100, Arb.string(0..150)) { name ->
                val food = Food(name = name, quantity = 1.0)
                val result = useCase(food)
                if (name.isBlank() || name.length > ValidateFoodUseCase.MAX_NAME_LENGTH) {
                    assertTrue("Expected NAME error for name='$name'", result.errors.containsKey(FoodField.NAME))
                } else {
                    assertFalse("Expected no NAME error for name='$name'", result.errors.containsKey(FoodField.NAME))
                }
            }
        }
    }

    @Test
    fun should_flagQuantityError_when_quantityIsOutOfRange() {
        runBlocking {
            checkAll(100, Arb.double(includeNonFiniteEdgeCases = false)) { quantity ->
                val food = Food(name = "Leche", quantity = quantity)
                val result = useCase(food)
                if (quantity < ValidateFoodUseCase.MIN_QUANTITY || quantity > ValidateFoodUseCase.MAX_QUANTITY) {
                    assertTrue("Expected QUANTITY error for quantity=$quantity", result.errors.containsKey(FoodField.QUANTITY))
                } else {
                    assertFalse("Expected no QUANTITY error for quantity=$quantity", result.errors.containsKey(FoodField.QUANTITY))
                }
            }
        }
    }

    @Test
    fun should_flagExpiryDateError_when_dateIsBeforeToday() {
        val today = LocalDate.now()
        runBlocking {
            checkAll(
                100,
                Arb.localDate(
                    minDate = today.minusYears(10),
                    maxDate = today.plusYears(10)
                )
            ) { date ->
                val food = Food(name = "Leche", quantity = 1.0, expiryDate = date)
                val result = useCase(food)
                if (date.isBefore(today)) {
                    assertTrue("Expected EXPIRY_DATE error for date=$date", result.errors.containsKey(FoodField.EXPIRY_DATE))
                } else {
                    assertFalse("Expected no EXPIRY_DATE error for date=$date", result.errors.containsKey(FoodField.EXPIRY_DATE))
                }
            }
        }
    }

    @Test
    fun should_guaranteeNonEmptyNameAndPositiveQuantity_when_resultIsValid() {
        // R26: storage manager guarantees non-empty name and quantity > 0 for every valid food
        runBlocking {
            checkAll(
                100,
                Arb.string(0..150),
                Arb.double(includeNonFiniteEdgeCases = false)
            ) { name, quantity ->
                val food = Food(name = name, quantity = quantity)
                val result = useCase(food)
                if (result.isValid) {
                    assertFalse("R26: valid food must have non-blank name", name.isBlank())
                    assertTrue("R26: valid food must have quantity > 0", quantity > 0.0)
                }
            }
        }
    }
}
