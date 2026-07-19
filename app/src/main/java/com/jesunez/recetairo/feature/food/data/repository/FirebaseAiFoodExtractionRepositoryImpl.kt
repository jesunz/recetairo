package com.jesunez.recetairo.feature.food.data.repository

import com.google.firebase.ai.GenerativeModel
import com.jesunez.recetairo.core.domain.model.Result
import com.jesunez.recetairo.feature.food.data.dto.AiFoodItemDto
import com.jesunez.recetairo.feature.food.data.mapper.toDomain
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.feature.food.domain.repository.AiFoodExtractionRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject

class FirebaseAiFoodExtractionRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val moshi: Moshi
) : AiFoodExtractionRepository {

    override suspend fun extractFoodItems(rawText: String): Result<List<OcrFoodItem>> = try {
        val prompt = PROMPT_TEMPLATE.replace(RAW_TEXT_PLACEHOLDER, rawText)
        val response = generativeModel.generateContent(prompt)
        val json = response.text
            ?: return Result.Error(IllegalStateException("Empty AI response"))

        val adapter = moshi.adapter<List<AiFoodItemDto>>(
            Types.newParameterizedType(List::class.java, AiFoodItemDto::class.java)
        )
        val dtos = adapter.fromJson(json).orEmpty()

        Result.Success(dtos.toDomain())
    } catch (e: Exception) {
        Result.Error(e)
    }

    private companion object {
        const val RAW_TEXT_PLACEHOLDER = "{{RAW_TEXT}}"

        val PROMPT_TEMPLATE = """
            You are a grocery receipt parser. You will receive raw OCR text extracted
            from a Spanish supermarket receipt. Extract ONLY grocery food items intended
            for human consumption.

            STRICT RULES:
            - Return ONLY food and beverage products (things people eat or drink):
              dairy, meat, fish, seafood, fruit, vegetables, bread, cereals/grains, and
              any other edible grocery item.
            - DO NOT return non-food products under any circumstances, including but not
              limited to: cleaning products, detergents, toiletries, cosmetics, hygiene
              products (soap, shampoo, toothpaste, diapers, sanitary products, etc.),
              household items, stationery, tobacco, plastic bags, pet food, or any line
              that is not something a person eats or drinks.
            - DO NOT return totals, subtotals, taxes, discounts, store name/address,
              dates, payment method, loyalty card numbers, receipt/ticket numbers,
              barcodes, or any other non-product line.
            - If a line is totally ambiguous (no indication at all that it is a food
              product), OMIT it rather than guessing.
            - If a line looks like a food product but its name is incomplete or
              corrupted by OCR noise (missing letters, truncated word, stray symbols)
              so that you cannot reconstruct it with confidence, DO NOT omit it and
              DO NOT complete or guess the missing characters. Instead, return it with
              "needsReview": true and "name" set to exactly the text as it appears in
              the source, cleaned only of stray OCR noise that is clearly not part of
              any word.
            - For each food item, extract:
              - "name": the product name as written, cleaned of stray OCR noise. Do not
                translate it or invent words that are not in the source text, not even
                when "needsReview" is true.
              - "quantity": the quantity or pack size if clearly present in the line
                (e.g. "2", "500g", "6x125ml"); use an empty string if not discernible.
              - "category": exactly one of: "lácteos", "carne", "pescado", "marisco",
                "frutas", "verduras", "pan", "cereales", "otros". Use "otros" only when
                none of the other 8 categories clearly apply.
              - "needsReview": true only for incomplete/noisy-but-plausible food names
                as described above; false (or omit the field) for every other item.
              - "emoji": OPTIONAL. If you can identify a single Unicode emoji character
                that specifically represents this exact food item (e.g. 🧀 for a cheese
                product, 🥛 for milk, 🍞 for bread), return it as-is in this field, as
                the literal emoji character itself — never a name or a shortcode such as
                ":cheese:". You are not restricted to a fixed list; pick whichever
                single emoji best and most specifically represents the item. If you
                cannot find one that clearly and specifically fits, omit the field
                entirely rather than guessing or using a generic placeholder.
            - Output must be a JSON array matching the provided schema. Do not include
              any text, explanation, or markdown outside the JSON array. If no food
              items are found, return an empty array.

            Receipt OCR text:
            $RAW_TEXT_PLACEHOLDER
        """.trimIndent()
    }
}
