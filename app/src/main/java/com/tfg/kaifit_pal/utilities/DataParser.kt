package com.tfg.kaifit_pal.utilities

class DataParser private constructor() {
    init {
        throw IllegalStateException("Utility class")
    }

    companion object {
        fun parseDoubleUtility(str: String): Double {
            return try {
                str.toDouble()
            } catch (e: NumberFormatException) {
                0
            }
        }

        fun parseIntUtility(str: String): Int {
            return try {
                str.toInt()
            } catch (e: NumberFormatException) {
                0
            }
        }
    }
}