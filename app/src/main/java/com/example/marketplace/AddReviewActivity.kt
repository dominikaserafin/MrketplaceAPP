package com.example.marketplace

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for adding product reviews
 */
class AddReviewActivity : Activity() {

    private lateinit var textViewProductToReview: TextView
    private lateinit var textViewCancel: TextView
    private lateinit var ratingBarReview: RatingBar
    private lateinit var editTextReviewComment: EditText
    private lateinit var buttonSubmitReview: Button
    private lateinit var buttonMenu: Button
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var auth: FirebaseAuth

    private var productId: Long = -1
    private var productName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_review)

        firebaseRepository = FirebaseRepository(this)
        auth = FirebaseAuth.getInstance()

        textViewProductToReview = findViewById(R.id.textViewProductToReview)
        textViewCancel = findViewById(R.id.textViewCancel)
        ratingBarReview = findViewById(R.id.ratingBarReview)
        editTextReviewComment = findViewById(R.id.editTextReviewComment)
        buttonSubmitReview = findViewById(R.id.buttonSubmitReview)
        buttonMenu = findViewById<Button>(R.id.buttonMenu)

        productId = intent.getLongExtra("product_id", -1)
        productName = intent.getStringExtra("product_name") ?: ""

        if (productId == -1L || productName.isEmpty()) {
            finish()
            return
        }

        textViewProductToReview.text = "Product: $productName"

        if (auth.currentUser == null) {
            finish()
            return
        }

        checkIfUserAlreadyReviewed()

        buttonSubmitReview.setOnClickListener {
            submitReview()
        }

        textViewCancel.setOnClickListener {
            finish()
        }

        buttonMenu.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Menu")
            builder.setMessage("To be developed")
            builder.setPositiveButton("OK", null)
            builder.show()
        }
    }

    /**
     * Check if user already reviewed this product
     */
    private fun checkIfUserAlreadyReviewed() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alreadyReviewed = firebaseRepository.hasUserReviewedProduct(productId)

                withContext(Dispatchers.Main) {
                    if (alreadyReviewed) {
                        buttonSubmitReview.isEnabled = false
                        buttonSubmitReview.text = "Already Reviewed"
                        ratingBarReview.isEnabled = false
                        editTextReviewComment.isEnabled = false
                        editTextReviewComment.hint = "You Have Already Reviewed This Product"
                        textViewProductToReview.text = "Product: $productName\n⚠️ You Already Reviewed This Product"
                    }
                }
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }

    /**
     * Submit review to database
     */
    private fun submitReview() {
        val rating = ratingBarReview.rating
        val comment = editTextReviewComment.text.toString().trim()

        if (!validateInput(rating, comment)) {
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        buttonSubmitReview.isEnabled = false
        buttonSubmitReview.text = "Submitting..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userInfo = firebaseRepository.getCurrentUserInfo()
                val username = userInfo?.get("name") as? String ?: currentUser.displayName ?: "Anonymous User"

                val review = Review(
                    productId = productId,
                    username = username,
                    rating = rating,
                    comment = comment,
                    date = getCurrentDate()
                )

                val success = firebaseRepository.addReview(review)

                withContext(Dispatchers.Main) {
                    buttonSubmitReview.isEnabled = true
                    buttonSubmitReview.text = "Submit Review"

                    if (success) {
                        clearFields()
                        finish()
                    } else {
                        val alreadyReviewed = firebaseRepository.hasUserReviewedProduct(productId)
                        if (alreadyReviewed) {
                            buttonSubmitReview.isEnabled = false
                            buttonSubmitReview.text = "Already Reviewed"
                            ratingBarReview.isEnabled = false
                            editTextReviewComment.isEnabled = false
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    buttonSubmitReview.isEnabled = true
                    buttonSubmitReview.text = "Submit Review"
                }
            }
        }
    }

    /**
     * Validate user input
     * @param rating Rating value
     * @param comment Comment text
     * @return true if input is valid
     */
    private fun validateInput(rating: Float, comment: String): Boolean {
        if (rating == 0f) {
            return false
        }

        if (comment.isEmpty()) {
            return false
        }

        if (comment.length < 10) {
            return false
        }

        if (comment.length > 500) {
            return false
        }


        return true
    }

    /**
     * Clear input fields
     */
    private fun clearFields() {
        ratingBarReview.rating = 0f
        editTextReviewComment.text.clear()
    }

    /**
     * Get current date as string
     * @return Current date in YYYY-MM-DD format
     */
    private fun getCurrentDate(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date())
    }

    override fun onBackPressed() {
        if (ratingBarReview.rating > 0 || editTextReviewComment.text.isNotEmpty()) {
            // Silent handling
        }
        super.onBackPressed()
        finish()
    }

    override fun onPause() {
        super.onPause()
    }
}