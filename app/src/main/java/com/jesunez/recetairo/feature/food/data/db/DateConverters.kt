package com.jesunez.recetairo.feature.food.data.db

import androidx.room.TypeConverter
import java.time.LocalDate

class DateConverters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
}
