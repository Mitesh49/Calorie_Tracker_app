package com.example.calorie_1

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Meal(
    val name: String,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double
) {
    companion object {
        // Convert a list of Meal objects to JSON
        fun toJson(meals: List<Meal>): String {
            return Gson().toJson(meals)
        }

        // Convert JSON string back to a list of Meal objects
        fun fromJson(json: String): List<Meal> {
            val type = object : TypeToken<List<Meal>>() {}.type
            return Gson().fromJson(json, type)
        }
    }
}
