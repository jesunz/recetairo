package com.jesunez.recetairo.feature.food.domain.usecase

import com.jesunez.recetairo.feature.food.domain.model.Food
import com.jesunez.recetairo.feature.food.domain.model.FoodField
import com.jesunez.recetairo.feature.food.domain.model.ValidationResult
import java.time.LocalDate
import javax.inject.Inject

class ValidateFoodUseCase @Inject constructor() {

    operator fun invoke(food: Food): ValidationResult {
        val errors = mutableMapOf<FoodField, String>()

        if (food.name.isBlank()) {
            errors[FoodField.NAME] = "El nombre no puede estar vacío"
        } else if (food.name.length > MAX_NAME_LENGTH) {
            errors[FoodField.NAME] = "El nombre no puede superar $MAX_NAME_LENGTH caracteres"
        }

        if (food.quantity !in MIN_QUANTITY..MAX_QUANTITY) {
            errors[FoodField.QUANTITY] = "La cantidad debe estar entre $MIN_QUANTITY y $MAX_QUANTITY"
        }

        if (food.unit.isBlank()) {
            errors[FoodField.UNIT] = "La unidad de medida no puede estar vacía"
        }

        food.expiryDate?.let { date ->
            if (date.isBefore(LocalDate.now())) {
                errors[FoodField.EXPIRY_DATE] = "La fecha de caducidad no puede ser anterior a hoy"
            }
        }

        return ValidationResult(isValid = errors.isEmpty(), errors = errors)
    }

    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MIN_QUANTITY = 0.001
        const val MAX_QUANTITY = 99.999
    }
}
