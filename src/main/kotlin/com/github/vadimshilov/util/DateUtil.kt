package com.github.vadimshilov.util

import java.time.LocalDate

object DateUtil {
    fun getDateInDays() =
            (LocalDate.now().toEpochDay() - LocalDate.of(1,1,1).toEpochDay() + 1).toInt()

}