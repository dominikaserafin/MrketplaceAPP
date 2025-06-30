package com.example.marketplace

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Entry point for user authentication
 */
class LoginActivity : Activity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonRegister: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        editTextEmail = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegister = findViewById(R.id.buttonRegisterNow)
    }

    private fun setupClickListeners() {
        buttonLogin.setOnClickListener { loginUser() }
        buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }
        findViewById<TextView>(R.id.textViewRegister).setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }
    }

    /**
     * Authenticates user and redirects based on account type
     */
    private fun loginUser() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                val notificationHelper = NotificationHelper(this)
                notificationHelper.clearSessionFlag()

                checkUserTypeAndRedirect()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Login failed - wrong password", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUserTypeAndRedirect() {
        val userId = auth.currentUser?.uid ?: return

        Firebase.firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val userType = document.getString("userType") ?: "buyer"

                getSharedPreferences("user_data", MODE_PRIVATE)
                    .edit()
                    .putString("user_type", userType)
                    .apply()

                val targetActivity = if (userType == "seller") {
                    SellerActivity::class.java
                } else {
                    BuyerActivity::class.java
                }

                startActivity(Intent(this, targetActivity))
                finish()
            }
            .addOnFailureListener {
                startActivity(Intent(this, BuyerActivity::class.java))
                finish()
            }
    }
}