package com.example.marketplace

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Allows sellers to add new products to their inventory
 */
class AddProductActivity : Activity() {

    private lateinit var editTextProductName: EditText
    private lateinit var editTextProductPrice: EditText
    private lateinit var editTextProductDescription: EditText
    private lateinit var editTextProductQuantity: EditText
    private lateinit var editTextCompanyName: EditText
    private lateinit var buttonAddProductSubmit: Button
    private lateinit var textViewCancel: TextView
    private lateinit var buttonMenu: Button
    private lateinit var buttonBack: Button
    private lateinit var firebaseRepository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        firebaseRepository = FirebaseRepository(this)
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        editTextProductName = findViewById(R.id.editTextProductName)
        editTextProductPrice = findViewById(R.id.editTextProductPrice)
        editTextProductDescription = findViewById(R.id.editTextProductDescription)
        editTextProductQuantity = findViewById(R.id.editTextProductQuantity)
        editTextCompanyName = findViewById(R.id.editTextCompanyName)
        buttonAddProductSubmit = findViewById(R.id.buttonAddProductSubmit)
        textViewCancel = findViewById(R.id.textViewCancel)
        buttonMenu = findViewById(R.id.buttonMenu)
        buttonBack = findViewById(R.id.buttonBack)
    }

    private fun setupClickListeners() {
        buttonAddProductSubmit.setOnClickListener { addProduct() }
        textViewCancel.setOnClickListener { goBack() }
        buttonMenu.setOnClickListener { showMenu() }
        buttonBack.setOnClickListener { goBack() }
    }

    private fun showMenu() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Add Product Menu")
        builder.setMessage("Additional features coming soon!")
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    /**
     * Checks for unsaved changes before navigating away
     */
    private fun goBack() {
        val hasChanges = editTextProductName.text.isNotEmpty() ||
                editTextProductPrice.text.isNotEmpty() ||
                editTextProductDescription.text.isNotEmpty() ||
                editTextProductQuantity.text.isNotEmpty() ||
                editTextCompanyName.text.isNotEmpty()

        if (hasChanges) {
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Discard Changes")
            builder.setMessage("You have unsaved changes. Are you sure you want to go back?")
            builder.setPositiveButton("Yes, Discard") { _, _ -> finish() }
            builder.setNegativeButton("No, Continue Editing", null)
            builder.show()
        } else {
            finish()
        }
    }

    /**
     * Validates input and creates new product in Firebase
     */
    private fun addProduct() {
        val name = editTextProductName.text.toString().trim()
        val priceStr = editTextProductPrice.text.toString().trim()
        val description = editTextProductDescription.text.toString().trim()
        val quantityStr = editTextProductQuantity.text.toString().trim()
        val companyName = editTextCompanyName.text.toString().trim()

        if (!validateInput(name, priceStr, description, quantityStr, companyName)) {
            return
        }

        val price: Double
        val quantity: Int
        try {
            price = priceStr.toDouble()
            quantity = quantityStr.toInt()
        } catch (e: NumberFormatException) {
            return
        }

        if (price <= 0 || quantity <= 0) {
            return
        }

        buttonAddProductSubmit.isEnabled = false
        buttonAddProductSubmit.text = "Adding Product..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val placeholderImageUrl = "https://via.placeholder.com/150x150.png?text=${name.replace(" ", "+")}"

                val product = Product(
                    id = 0,
                    name = name,
                    price = price,
                    description = description,
                    quantity = quantity,
                    imageUrl = placeholderImageUrl,
                    sellerId = "",
                    sellerCompany = companyName
                )

                val success = firebaseRepository.addProduct(product)

                withContext(Dispatchers.Main) {
                    buttonAddProductSubmit.isEnabled = true
                    buttonAddProductSubmit.text = "Add A Product"

                    if (success) {
                        Toast.makeText(this@AddProductActivity, "Product added successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        clearFields()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    buttonAddProductSubmit.isEnabled = true
                    buttonAddProductSubmit.text = "Add A Product"
                }
            }
        }
    }

    private fun clearFields() {
        editTextProductName.text.clear()
        editTextProductPrice.text.clear()
        editTextProductDescription.text.clear()
        editTextProductQuantity.text.clear()
        editTextCompanyName.text.clear()
        editTextProductName.requestFocus()
    }

    /**
     * Basic validation for product data
     * @return true if all fields are properly filled
     */
    private fun validateInput(name: String, priceStr: String, description: String,
                              quantityStr: String, companyName: String): Boolean {
        if (name.isEmpty() || priceStr.isEmpty() || description.isEmpty() ||
            quantityStr.isEmpty() || companyName.isEmpty()) {
            return false
        }

        if (name.length < 3 || description.length < 10 || companyName.length < 2) {
            return false
        }

        return true
    }

    override fun onBackPressed() {
        goBack()
    }
}