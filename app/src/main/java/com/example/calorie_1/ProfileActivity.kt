package com.example.calorie_1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        // Fetch and display email
        val emailTextView = findViewById<TextView>(R.id.profile_email)
        val currentUser = auth.currentUser
        emailTextView.text = "Email: ${currentUser?.email}"

        // Sign out button
        findViewById<Button>(R.id.btn_sign_out).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        // Bottom Navigation setup
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_profile
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_profile -> {
                    // Already in ProfileActivity
                    true
                }
                R.id.navigation_camera -> {
                    startActivity(Intent(this, CaptureActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }


}
