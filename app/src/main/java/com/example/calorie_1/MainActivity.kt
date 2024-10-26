package com.example.calorie_1

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var todayMealsContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var goalCaloriesTextView: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    private var totalCalories = 0.0
    private var totalProteins = 0.0
    private var totalFats = 0.0
    private var totalCarbs = 0.0
    private var targetCalories = 0.0 // Goal Calories

    private val todayMealsList = mutableListOf<Meal>() // List to store meals

    private lateinit var noMealsTextView: TextView // TextView to show if no meals added

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("CalorieTracker", MODE_PRIVATE)

        todayMealsContainer = findViewById(R.id.meal_container)
        progressBar = findViewById(R.id.progress_circle)
        goalCaloriesTextView = findViewById(R.id.tv_goal_calories)
        noMealsTextView = findViewById(R.id.tv_no_meals) // Initialize the TextView

        loadPreferences() // Load saved data

        findViewById<Button>(R.id.btn_add_meals).setOnClickListener {
            openMealInput()
        }

        findViewById<Button>(R.id.btn_set_target).setOnClickListener {
            val targetCaloriesInput = findViewById<EditText>(R.id.et_target_calories).text.toString().toDoubleOrNull() ?: 0.0
            targetCalories = targetCaloriesInput

            // Update the goal calories text
            goalCaloriesTextView.text = "Goal: $targetCalories kcal"
            updateProgressBar()
            savePreferences() // Save updated data
        }

        // Bottom Navigation Bar setup
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

        // Initially show the "No meals added yet" message if the list is empty
        updateNoMealsMessage()
    }

    private fun openMealInput() {
        val mealInputView = layoutInflater.inflate(R.layout.item_meal, null)

        mealInputView.findViewById<Button>(R.id.btn_add).setOnClickListener {
            val mealName = mealInputView.findViewById<EditText>(R.id.et_meal_name).text.toString()
            val mealCalories = mealInputView.findViewById<EditText>(R.id.et_meal_calories).text.toString().toDoubleOrNull() ?: 0.0
            val mealProteins = mealInputView.findViewById<EditText>(R.id.et_meal_proteins).text.toString().toDoubleOrNull() ?: 0.0
            val mealFats = mealInputView.findViewById<EditText>(R.id.et_meal_fats).text.toString().toDoubleOrNull() ?: 0.0
            val mealCarbs = mealInputView.findViewById<EditText>(R.id.et_meal_carbs).text.toString().toDoubleOrNull() ?: 0.0

            // Add meal to the list
            val meal = Meal(mealName, mealCalories, mealProteins, mealFats, mealCarbs)
            todayMealsList.add(meal)

            // Update totals after adding the meal
            updateTotalValues(mealCalories, mealProteins, mealFats, mealCarbs)

            // Update UI to show today's meals
            displayMeals()

            // Clear input fields
            mealInputView.findViewById<EditText>(R.id.et_meal_name).text.clear()
            mealInputView.findViewById<EditText>(R.id.et_meal_calories).text.clear()
            mealInputView.findViewById<EditText>(R.id.et_meal_proteins).text.clear()
            mealInputView.findViewById<EditText>(R.id.et_meal_fats).text.clear()
            mealInputView.findViewById<EditText>(R.id.et_meal_carbs).text.clear()

            mealInputView.visibility = View.GONE
            savePreferences() // Save updated data
        }

        todayMealsContainer.addView(mealInputView)
    }

    private fun displayMeals() {
        todayMealsContainer.removeAllViews() // Clear previous views
        for (meal in todayMealsList) {
            val mealView = layoutInflater.inflate(R.layout.item_meal_row, todayMealsContainer, false)

            mealView.findViewById<TextView>(R.id.tv_meal_name).text = meal.name
            mealView.findViewById<TextView>(R.id.tv_meal_calories).text = "${meal.calories} kcal"
            mealView.findViewById<TextView>(R.id.tv_meal_proteins).text = "${meal.proteins} gr"
            mealView.findViewById<TextView>(R.id.tv_meal_fats).text = "${meal.fats} gr"
            mealView.findViewById<TextView>(R.id.tv_meal_carbs).text = "${meal.carbs} gr"

            mealView.findViewById<Button>(R.id.btn_delete_log).setOnClickListener {
                // Remove meal from list and update UI
                todayMealsList.remove(meal)
                displayMeals()
                // Update total values when a meal is removed
                updateTotalValues(-meal.calories, -meal.proteins, -meal.fats, -meal.carbs)
                savePreferences() // Save updated data
            }

            todayMealsContainer.addView(mealView)
        }

        // Update "No meals added yet" message based on the list
        updateNoMealsMessage()

        // Update total values based on all meals
        updateTotalValuesForAllMeals()
    }

    private fun updateNoMealsMessage() {
        if (todayMealsList.isEmpty()) {
            noMealsTextView.visibility = View.VISIBLE // Show message if no meals added
        } else {
            noMealsTextView.visibility = View.GONE // Hide message if meals are present
        }
    }

    private fun updateTotalValues(calories: Double, proteins: Double, fats: Double, carbs: Double) {
        totalCalories += calories
        totalProteins += proteins
        totalFats += fats
        totalCarbs += carbs

        // Update the displayed totals
        findViewById<TextView>(R.id.tv_calories).text = totalCalories.toString()
        findViewById<TextView>(R.id.protein_info).text = "Proteins\n${totalProteins} gr"
        findViewById<TextView>(R.id.fats_info).text = "Fats\n${totalFats} gr"
        findViewById<TextView>(R.id.carbs_info).text = "Carbs\n${totalCarbs} gr"

        updateProgressBar() // Update the progress bar whenever the total values change
    }

    private fun updateTotalValuesForAllMeals() {
        totalCalories = todayMealsList.sumOf { it.calories }
        totalProteins = todayMealsList.sumOf { it.proteins }
        totalFats = todayMealsList.sumOf { it.fats }
        totalCarbs = todayMealsList.sumOf { it.carbs }

        updateTotalValues(0.0, 0.0, 0.0, 0.0) // Call to refresh the UI display
        updateProgressBar()
    }

    private fun updateProgressBar() {
        if (targetCalories > 0) {
            val progress = (totalCalories / targetCalories * 100).toInt()
            progressBar.progress = progress.coerceAtMost(100) // Ensures progress doesn't exceed 100%
        }
    }

    private fun savePreferences() {
        val editor = sharedPreferences.edit()
        editor.putFloat("totalCalories", totalCalories.toFloat())
        editor.putFloat("totalProteins", totalProteins.toFloat())
        editor.putFloat("totalFats", totalFats.toFloat())
        editor.putFloat("totalCarbs", totalCarbs.toFloat())
        editor.putFloat("targetCalories", targetCalories.toFloat())

        // Save today's meals as a String (serialize manually)
        val mealsString = todayMealsList.joinToString(";") { "${it.name},${it.calories},${it.proteins},${it.fats},${it.carbs}" }
        editor.putString("todayMeals", mealsString)

        editor.apply()
    }

    private fun loadPreferences() {
        totalCalories = sharedPreferences.getFloat("totalCalories", 0f).toDouble()
        totalProteins = sharedPreferences.getFloat("totalProteins", 0f).toDouble()
        totalFats = sharedPreferences.getFloat("totalFats", 0f).toDouble()
        totalCarbs = sharedPreferences.getFloat("totalCarbs", 0f).toDouble()
        targetCalories = sharedPreferences.getFloat("targetCalories", 0f).toDouble()

        goalCaloriesTextView.text = "Goal: $targetCalories kcal"

        // Load today's meals from SharedPreferences
        val mealsString = sharedPreferences.getString("todayMeals", "") ?: ""
        if (mealsString.isNotEmpty()) {
            mealsString.split(";").forEach {
                val mealData = it.split(",")
                if (mealData.size == 5) {
                    val meal = Meal(
                        name = mealData[0],
                        calories = mealData[1].toDouble(),
                        proteins = mealData[2].toDouble(),
                        fats = mealData[3].toDouble(),
                        carbs = mealData[4].toDouble()
                    )
                    todayMealsList.add(meal)
                }
            }
        }

        // Update UI based on loaded data
        displayMeals()
        updateProgressBar()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.navigation_home -> {
                true
            }
            R.id.navigation_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.navigation_camera -> {
                val intent = Intent(this, CaptureActivity::class.java)
                startActivity(intent)
                true
            }
            else -> false
        }
    }

    data class Meal(
        val name: String,
        val calories: Double,
        val proteins: Double,
        val fats: Double,
        val carbs: Double
    )
}
