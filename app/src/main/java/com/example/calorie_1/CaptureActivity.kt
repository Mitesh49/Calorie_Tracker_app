package com.example.calorie_1

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.calorie_1.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException

class CaptureActivity : AppCompatActivity() {

    private lateinit var selectImageButton: Button
    private lateinit var captureButton: Button
    private lateinit var makePredictionButton: Button
    private lateinit var addMealButton: Button
    private lateinit var capturedImageView: ImageView
    private lateinit var predictionTextView: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    private val CAMERA_REQUEST_CODE = 101
    private val GALLERY_REQUEST_CODE = 102
    private lateinit var capturedBitmap: Bitmap

    private val mealDetails = mapOf(
        "apple" to MealDetails("Apple", 95.0, 0.5, 0.3, 25.0),
        "banana" to MealDetails("Banana", 105.0, 1.3, 0.4, 27.0),
        "orange" to MealDetails("Orange", 62.0, 1.2, 0.2, 15.0),
        "cucumber" to MealDetails("Cucumber", 16.0, 0.7, 0.1, 4.0),
        "potato" to MealDetails("Potato", 163.0, 4.3, 0.2, 37.0),
        "carrot" to MealDetails("Carrot", 41.0, 0.9, 0.2, 10.0)
    )

    private data class MealDetails(val name: String, val calories: Double, val protein: Double, val fat: Double, val carbs: Double)

    private val validLabels = setOf("apple", "banana", "orange", "cucumber", "potato", "carrot")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        selectImageButton = findViewById(R.id.button)
        captureButton = findViewById(R.id.camerabtn)
        makePredictionButton = findViewById(R.id.button2)
        addMealButton = findViewById(R.id.addButton)
        capturedImageView = findViewById(R.id.imageView2)
        predictionTextView = findViewById(R.id.textView)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        checkPermissions()

        selectImageButton.setOnClickListener {
            selectImageFromGallery()
        }

        captureButton.setOnClickListener {
            openCamera()
        }

        makePredictionButton.setOnClickListener {
            if (::capturedBitmap.isInitialized) {
                classifyImage(capturedBitmap)
            } else {
                Toast.makeText(this, "Please capture or select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        addMealButton.setOnClickListener {
            val predictedLabel = predictionTextView.text.toString()
            val mealDetail = mealDetails[predictedLabel]

            if (mealDetail != null) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("mealName", mealDetail.name)
                    putExtra("calories", mealDetail.calories)
                    putExtra("protein", mealDetail.protein)
                    putExtra("fat", mealDetail.fat)
                    putExtra("carbs", mealDetail.carbs)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "No valid meal detected", Toast.LENGTH_SHORT).show()
            }
        }

        setupBottomNavigation()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), GALLERY_REQUEST_CODE)
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    val photo = data?.extras?.get("data") as? Bitmap
                    photo?.let {
                        capturedBitmap = it
                        capturedImageView.setImageBitmap(it)
                        capturedImageView.visibility = ImageView.VISIBLE
                    } ?: run {
                        Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    val uri: Uri? = data?.data
                    uri?.let {
                        try {
                            capturedBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, it)
                            capturedImageView.setImageBitmap(capturedBitmap)
                            capturedImageView.visibility = ImageView.VISIBLE
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun classifyImage(image: Bitmap) {
        val resized = Bitmap.createScaledBitmap(image, 224, 224, true)
        val model = Model.newInstance(this)

        val tbuffer = TensorImage.fromBitmap(resized)
        val byteBuffer = tbuffer.buffer

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.UINT8)
        inputFeature0.loadBuffer(byteBuffer)

        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        val maxIndex = getMax(outputFeature0.floatArray)

        val allLabels = application.assets.open("labels.txt").bufferedReader().use { it.readLines() }

        val predictedLabel = allLabels[maxIndex]

        if (validLabels.contains(predictedLabel)) {
            val mealDetail = mealDetails[predictedLabel]
            if (mealDetail != null) {
                val mealInfo = """
                    ${mealDetail.name}
                    Calories: ${mealDetail.calories}
                    Protein: ${mealDetail.protein}g
                    Fat: ${mealDetail.fat}g
                    Carbs: ${mealDetail.carbs}g
                """.trimIndent()
                predictionTextView.text = mealInfo
            } else {
                predictionTextView.text = "Meal details not available."
            }
        } else {
            predictionTextView.text = "Cannot detect."
        }

        model.close()
    }

    private fun getMax(array: FloatArray): Int {
        var maxIndex = 0
        for (i in array.indices) {
            if (array[i] > array[maxIndex]) {
                maxIndex = i
            }
        }
        return maxIndex
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_camera -> {
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}