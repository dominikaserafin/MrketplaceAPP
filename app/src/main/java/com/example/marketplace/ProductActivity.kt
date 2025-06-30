package com.example.marketplace

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shows detailed product information, reviews, and purchase options
 */
class ProductActivity : Activity() {

    private lateinit var textViewProductName: TextView
    private lateinit var textViewPrice: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var textViewQuantity: TextView
    private lateinit var textViewNoReviews: TextView
    private lateinit var buttonAddReview: Button
    private lateinit var buttonAddToCart: Button
    private lateinit var buttonCart: Button
    private lateinit var buttonMenu: Button
    private lateinit var buttonBack: Button
    private lateinit var recyclerViewReviews: RecyclerView
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var firebaseRepository: FirebaseRepository

    private val reviewList = mutableListOf<Review>()
    private var productId: Long = -1
    private var productName: String = ""
    private var productPrice: Double = 0.0
    private var productQuantity: Int = 0
    private var firebaseProductId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        firebaseRepository = FirebaseRepository()

        initViews()
        getProductDataFromIntent()
        setupButtons()
        setupClickListeners()
        loadProductReviews()
    }

    private fun initViews() {
        textViewProductName = findViewById(R.id.textViewProductName)
        textViewPrice = findViewById(R.id.textViewPrice)
        textViewDescription = findViewById(R.id.textViewDescription)
        textViewQuantity = findViewById(R.id.textViewQuantity)
        textViewNoReviews = findViewById(R.id.textViewNoReviews)
        buttonAddReview = findViewById(R.id.buttonAddReview)
        buttonAddToCart = findViewById(R.id.buttonAddToCart)
        buttonCart = findViewById(R.id.buttonCart)
        buttonMenu = findViewById(R.id.buttonMenu)
        buttonBack = findViewById(R.id.buttonBack)
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews)

        recyclerViewReviews.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        buttonAddReview.setOnClickListener { checkUserEligibilityAndOpenReview() }
        buttonAddToCart.setOnClickListener { addToCart() }
        buttonCart.setOnClickListener { openCart() }
        buttonMenu.setOnClickListener { showMenu() }
        buttonBack.setOnClickListener { finish() }
    }

    private fun openCart() {
        startActivity(Intent(this, CartActivity::class.java))
    }

    private fun showMenu() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Product Menu")
        builder.setMessage("Additional features coming soon!")
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    private fun getProductDataFromIntent() {
        productId = intent.getLongExtra("product_id", -1)
        productName = intent.getStringExtra("product_name") ?: ""
        productPrice = intent.getDoubleExtra("product_price", 0.0)
        val productDescription = intent.getStringExtra("product_description") ?: ""
        productQuantity = intent.getIntExtra("product_quantity", 0)
        firebaseProductId = intent.getStringExtra("firebase_product_id") ?: ""

        displayProductInfo(productDescription)
    }

    private fun displayProductInfo(description: String) {
        textViewProductName.text = productName
        textViewPrice.text = "$${String.format("%.2f", productPrice)}"
        textViewDescription.text = description
        textViewQuantity.text = "Available quantity: $productQuantity"

        if (productId == -1L || productName.isEmpty()) {
            finish()
        }
    }

    /**
     * Configures buttons based on user type (seller vs buyer)
     */
    private fun setupButtons() {
        val isBuyer = checkIfUserIsBuyer()

        if (!isBuyer) {
            buttonCart.visibility = View.GONE
        } else {
            buttonCart.visibility = View.VISIBLE
        }

        if (!isBuyer) {
            buttonAddReview.visibility = View.GONE
        } else {
            buttonAddReview.visibility = View.VISIBLE
            buttonAddReview.text = "Add A Review"
        }

        if (!isBuyer) {
            buttonAddToCart.text = "Sellers Can't Buy"
            buttonAddToCart.isEnabled = false
        } else if (productQuantity <= 0) {
            buttonAddToCart.text = "Out of Stock"
            buttonAddToCart.isEnabled = false
        } else {
            buttonAddToCart.text = "Add To Cart"
            buttonAddToCart.isEnabled = true
        }
    }

    /**
     * Adds one item to cart, respecting quantity limits
     */
    private fun addToCart() {
        if (!checkIfUserIsBuyer()) {
            return
        }

        if (productQuantity <= 0) {
            return
        }

        if (firebaseProductId.isEmpty()) {
            return
        }

        val sharedPreferences = getSharedPreferences("cart_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val currentQuantityInCart = sharedPreferences.getInt("product_${productId}_quantity", 0)

        if (currentQuantityInCart >= productQuantity) {
            Toast.makeText(this,
                "Cannot add more. Maximum available: $productQuantity",
                Toast.LENGTH_LONG).show()
            return
        }

        val newQuantity = currentQuantityInCart + 1

        editor.putString("product_${productId}_name", productName)
        editor.putFloat("product_${productId}_price", productPrice.toFloat())
        editor.putString("product_${productId}_image", "")
        editor.putInt("product_${productId}_quantity", newQuantity)
        editor.putString("product_${productId}_firebase_id", firebaseProductId)
        editor.putInt("product_${productId}_max_quantity", productQuantity)

        val productsInCart = sharedPreferences.getStringSet("cart_product_ids", mutableSetOf()) ?: mutableSetOf()
        productsInCart.add(productId.toString())
        editor.putStringSet("cart_product_ids", productsInCart)

        editor.apply()
    }

    private fun loadProductReviews() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reviews = firebaseRepository.getReviewsForProduct(productId)

                withContext(Dispatchers.Main) {
                    reviewList.clear()
                    reviewList.addAll(reviews)
                    updateReviewsDisplay()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateReviewsDisplay()
                }
            }
        }
    }

    private fun updateReviewsDisplay() {
        if (reviewList.isEmpty()) {
            textViewNoReviews.visibility = View.VISIBLE
            recyclerViewReviews.visibility = View.GONE
        } else {
            textViewNoReviews.visibility = View.GONE
            recyclerViewReviews.visibility = View.VISIBLE

            reviewAdapter = ReviewAdapter(reviewList)
            recyclerViewReviews.adapter = reviewAdapter
        }
    }

    /**
     * Verifies user has purchased product before allowing review
     */
    private fun checkUserEligibilityAndOpenReview() {
        buttonAddReview.isEnabled = false
        buttonAddReview.text = "Checking..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alreadyReviewed = firebaseRepository.hasUserReviewedProduct(productId)

                if (alreadyReviewed) {
                    withContext(Dispatchers.Main) {
                        buttonAddReview.isEnabled = false
                        buttonAddReview.text = "Already Reviewed"
                    }
                    return@launch
                }

                val hasPurchased = firebaseRepository.hasUserPurchasedProduct(productId)

                withContext(Dispatchers.Main) {
                    buttonAddReview.isEnabled = true
                    buttonAddReview.text = "Add A Review"

                    if (hasPurchased) {
                        openAddReviewActivity()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    buttonAddReview.isEnabled = true
                    buttonAddReview.text = "Add A Review"
                }
            }
        }
    }

    private fun openAddReviewActivity() {
        val intent = Intent(this, AddReviewActivity::class.java)
        intent.putExtra("product_id", productId)
        intent.putExtra("product_name", productName)
        startActivity(intent)
    }

    private fun checkIfUserIsBuyer(): Boolean {
        val sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE)
        val userType = sharedPreferences.getString("user_type", "buyer")
        return userType == "buyer"
    }

    override fun onResume() {
        super.onResume()
        loadProductReviews()
        setupButtons()
    }

    override fun onBackPressed() {
        finish()
    }
}