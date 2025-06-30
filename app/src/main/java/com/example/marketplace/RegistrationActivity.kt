package com.example.marketplace

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Registration screen for new users
 */
class RegistrationActivity : Activity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextAge: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextRepeatPassword: EditText
    private lateinit var editTextCompanyName: EditText
    private lateinit var checkBoxBuyer: CheckBox
    private lateinit var checkBoxSeller: CheckBox
    private lateinit var buttonRegister: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupListeners()
    }

    /**
     * Initialize UI components
     */
    private fun initViews() {
        editTextName = findViewById(R.id.editTextName)
        editTextAge = findViewById(R.id.editTextAge)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextRegPassword)
        editTextRepeatPassword = findViewById(R.id.editTextRepeatPassword)
        editTextCompanyName = findViewById(R.id.editTextCompanyName)
        checkBoxBuyer = findViewById(R.id.checkBoxBuyer)
        checkBoxSeller = findViewById(R.id.checkBoxSeller)
        buttonRegister = findViewById(R.id.buttonRegister)

        editTextCompanyName.visibility = View.GONE
    }

    /**
     * Setup UI listeners
     */
    private fun setupListeners() {
        checkBoxBuyer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBoxSeller.isChecked = false
                editTextCompanyName.visibility = View.GONE
            }
        }

        checkBoxSeller.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBoxBuyer.isChecked = false
                editTextCompanyName.visibility = View.VISIBLE
            } else {
                editTextCompanyName.visibility = View.GONE
            }
        }

        buttonRegister.setOnClickListener { registerUser() }

        findViewById<TextView>(R.id.textViewBackToLogin).setOnClickListener { finish() }
    }

    /**
     * Handle user registration
     */
    private fun registerUser() {
        val name = editTextName.text.toString().trim()
        val age = editTextAge.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()
        val repeatPassword = editTextRepeatPassword.text.toString().trim()
        val companyName = editTextCompanyName.text.toString().trim()

        // Basic validation
        if (name.isEmpty() || age.isEmpty() || email.isEmpty() ||
            password.isEmpty() || repeatPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != repeatPassword) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show()
            return
        }

        if (!checkBoxBuyer.isChecked && !checkBoxSeller.isChecked) {
            Toast.makeText(this, "Please select user type", Toast.LENGTH_SHORT).show()
            return
        }

        if (checkBoxSeller.isChecked && companyName.isEmpty()) {
            Toast.makeText(this, "Please enter company name", Toast.LENGTH_SHORT).show()
            return
        }

        // Create account
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                saveUserData(name, age.toInt(), email, companyName)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Save user data to Firestore
     */
    private fun saveUserData(name: String, age: Int, email: String, companyName: String) {
        val userId = auth.currentUser?.uid ?: return
        val userType = if (checkBoxSeller.isChecked) "seller" else "buyer"

        val userData = hashMapOf(
            "name" to name,
            "email" to email,
            "age" to age,
            "userType" to userType
        )

        if (userType == "seller") {
            userData["companyName"] = companyName
        }

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                redirectToApp(userType)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Redirect to appropriate app screen
     */
    private fun redirectToApp(userType: String) {
        val targetActivity = if (userType == "seller") {
            SellerActivity::class.java
        } else {
            BuyerActivity::class.java
        }

        startActivity(Intent(this, targetActivity))
        finish()
    }
}