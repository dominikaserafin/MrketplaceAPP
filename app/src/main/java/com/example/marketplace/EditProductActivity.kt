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
 * Activity for editing existing products (sellers only)
 */
class EditProductActivity : Activity() {

    private lateinit var editTextProductName: EditText
    private lateinit var editTextProductPrice: EditText
    private lateinit var editTextProductDescription: EditText
    private lateinit var editTextProductQuantity: EditText
    private lateinit var editTextCompanyName: EditText
    private lateinit var buttonUpdateProduct: Button
    private lateinit var buttonDeleteProduct: Button
    private lateinit var textViewCancel: TextView
    private lateinit var buttonMenu: Button
    private lateinit var buttonBack: Button
    private lateinit var firebaseRepository: FirebaseRepository

    private var productId: Long = -1
    private var firebaseProductId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        firebaseRepository = FirebaseRepository(this)
        getProductDataFromIntent()

        if (productId == -1L || firebaseProductId.isEmpty()) {
            Toast.makeText(this, "Invalid product data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        fillProductData()
        setupClickListeners()
    }

    /**
     * Get product data from intent extras
     */
    private fun getProductDataFromIntent() {
        productId = intent.getLongExtra("product_id", -1)
        firebaseProductId = intent.getStringExtra("firebase_product_id") ?: ""
    }

    /**
     * Initialize UI components
     */
    private fun initViews() {
        editTextProductName = findViewById(R.id.editTextProductName)
        editTextProductPrice = findViewById(R.id.editTextProductPrice)
        editTextProductDescription = findViewById(R.id.editTextProductDescription)
        editTextProductQuantity = findViewById(R.id.editTextProductQuantity)
        editTextCompanyName = findViewById(R.id.editTextCompanyName)
        buttonUpdateProduct = findViewById(R.id.buttonUpdateProduct)
        buttonDeleteProduct = findViewById(R.id.buttonDeleteProduct)
        textViewCancel = findViewById(R.id.textViewCancel)
        buttonMenu = findViewById(R.id.buttonMenu)
        buttonBack = findViewById(R.id.buttonBack)
    }

    /**
     * Fill form fields with existing product data
     */
    private fun fillProductData() {
        val productName = intent.getStringExtra("product_name") ?: ""
        val productPrice = intent.getDoubleExtra("product_price", 0.0)
        val productDescription = intent.getStringExtra("product_description") ?: ""
        val productQuantity = intent.getIntExtra("product_quantity", 0)
        val productCompany = intent.getStringExtra("product_company") ?: ""

        editTextProductName.setText(productName)
        editTextProductPrice.setText(productPrice.toString())
        editTextProductDescription.setText(productDescription)
        editTextProductQuantity.setText(productQuantity.toString())
        editTextCompanyName.setText(productCompany)
    }

    /**
     * Setup button click listeners
     */
    private fun setupClickListeners() {
        buttonUpdateProduct.setOnClickListener { updateProduct() }
        buttonDeleteProduct.setOnClickListener { showDeleteConfirmation() }
        textViewCancel.setOnClickListener { goBack() }
        buttonMenu.setOnClickListener { showMenu() }
        buttonBack.setOnClickListener { goBack() }
    }

    /**
     * Show simple menu dialog
     */
    private fun showMenu() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Edit Product Menu")
        builder.setMessage("Additional features coming soon!")
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    /**
     * Handle back navigation
     */
    private fun goBack() {
        finish()
    }

    /**
     * Update product in database
     */
    private fun updateProduct() {
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
            Toast.makeText(this, "Invalid price or quantity format", Toast.LENGTH_SHORT).show()
            return
        }

        if (price <= 0 || quantity < 0) {
            Toast.makeText(this, "Price must be > 0 and quantity >= 0", Toast.LENGTH_SHORT).show()
            return
        }

        buttonUpdateProduct.isEnabled = false
        buttonUpdateProduct.text = "Updating..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create placeholder image URL
                val placeholderImageUrl = "https://via.placeholder.com/150x150.png?text=${name.replace(" ", "+")}"

                val updatedProduct = Product(
                    id = productId,
                    name = name,
                    price = price,
                    description = description,
                    quantity = quantity,
                    imageUrl = placeholderImageUrl,
                    sellerId = "",
                    sellerCompany = companyName,
                    firebaseProductId = firebaseProductId
                )

                val success = firebaseRepository.updateProduct(updatedProduct)

                withContext(Dispatchers.Main) {
                    buttonUpdateProduct.isEnabled = true
                    buttonUpdateProduct.text = "Update Product"

                    if (success) {
                        Toast.makeText(this@EditProductActivity,
                            "Product updated successfully!",
                            Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@EditProductActivity,
                            "Failed to update product",
                            Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    buttonUpdateProduct.isEnabled = true
                    buttonUpdateProduct.text = "Update Product"
                    Toast.makeText(this@EditProductActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Show delete confirmation dialog
     */
    private fun showDeleteConfirmation() {
        val productName = editTextProductName.text.toString()
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Delete Product")
        builder.setMessage("Are you sure you want to delete '$productName'?\n\nThis action cannot be undone!")
        builder.setPositiveButton("Yes, Delete") { _, _ -> deleteProduct() }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    /**
     * Delete product from database
     */
    private fun deleteProduct() {
        buttonDeleteProduct.isEnabled = false
        buttonDeleteProduct.text = "Deleting..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = firebaseRepository.deleteProduct(firebaseProductId)

                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@EditProductActivity,
                            "Product deleted successfully!",
                            Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@EditProductActivity,
                            "Failed to delete product",
                            Toast.LENGTH_LONG).show()
                        buttonDeleteProduct.isEnabled = true
                        buttonDeleteProduct.text = "Delete Product"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    buttonDeleteProduct.isEnabled = true
                    buttonDeleteProduct.text = "Delete Product"
                    Toast.makeText(this@EditProductActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Validate user input
     */
    private fun validateInput(name: String, priceStr: String, description: String,
                              quantityStr: String, companyName: String): Boolean {
        if (name.isEmpty() || priceStr.isEmpty() || description.isEmpty() ||
            quantityStr.isEmpty() || companyName.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        if (name.length < 3) {
            Toast.makeText(this, "Product name must be at least 3 characters", Toast.LENGTH_SHORT).show()
            return false
        }

        if (description.length < 10) {
            Toast.makeText(this, "Description must be at least 10 characters", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    override fun onBackPressed() {
        goBack()
    }
}