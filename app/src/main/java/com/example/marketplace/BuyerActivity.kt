package com.example.marketplace

import android.content.Intent
import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

/**
 * Main activity for buyer users
 */
class BuyerActivity : Activity() {

    private lateinit var buttonBrowseProducts: Button
    private lateinit var buttonHistory: Button
    private lateinit var buttonMenu: Button
    private lateinit var buttonCart: Button
    private lateinit var buttonLogout: Button
    private lateinit var textViewCartInfo: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buyer_new)

        auth = FirebaseAuth.getInstance()
        saveUserType("buyer")

        initViews()
        setupClickListeners()
        updateCartInfo()
    }

    /**
     * Initialize UI components
     */
    private fun initViews() {
        buttonBrowseProducts = findViewById(R.id.buttonBrowseProducts)
        buttonHistory = findViewById(R.id.buttonHistory)
        buttonMenu = findViewById(R.id.buttonMenu)
        buttonCart = findViewById(R.id.buttonCart)
        buttonLogout = findViewById(R.id.buttonLogout)
        textViewCartInfo = findViewById(R.id.textViewCartInfo)
    }

    /**
     * Setup button click listeners
     */
    private fun setupClickListeners() {
        buttonBrowseProducts.setOnClickListener {
            startActivity(Intent(this, BrowserActivity::class.java))
        }

        buttonHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        buttonMenu.setOnClickListener { showMenu() }
        buttonCart.setOnClickListener { openCart() }
        buttonLogout.setOnClickListener { showLogoutDialog() }
    }

    /**
     * Open cart activity
     */
    private fun openCart() {
        startActivity(Intent(this, CartActivity::class.java))
    }

    /**
     * Show simple menu dialog
     */
    private fun showMenu() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Menu")
        builder.setMessage("Additional features coming soon!")
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    /**
     * Update cart info display
     */
    private fun updateCartInfo() {
        val sharedPreferences = getSharedPreferences("cart_data", MODE_PRIVATE)
        val productIds = sharedPreferences.getStringSet("cart_product_ids", emptySet()) ?: emptySet()
        val cartItemCount = productIds.size

        textViewCartInfo.text = if (cartItemCount > 0) {
            "You have $cartItemCount item(s) in your cart. Tap 🛒 to view!"
        } else {
            "Your cart is empty. Browse products to start shopping!"
        }
    }

    /**
     * Show logout confirmation dialog
     */
    private fun showLogoutDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Logout Confirmation")
        builder.setMessage("Are you sure you want to logout?")
        builder.setPositiveButton("Yes") { _, _ -> performLogout() }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    /**
     * Perform logout operation
     */
    private fun performLogout() {
        // Clear notification session flag (even though buyers don't get notifications,
        // it's good to clear it in case they switch to seller account)
        val notificationHelper = NotificationHelper(this)
        notificationHelper.clearSessionFlag()

        auth.signOut()

        // Clear user data
        getSharedPreferences("user_data", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("cart_data", MODE_PRIVATE).edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Save user type to preferences
     * @param userType Type of user ("seller" or "buyer")
     */
    private fun saveUserType(userType: String) {
        getSharedPreferences("user_data", MODE_PRIVATE)
            .edit()
            .putString("user_type", userType)
            .apply()
    }

    override fun onBackPressed() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Exit App")
        builder.setMessage("Are you sure you want to exit?")
        builder.setPositiveButton("Yes") { _, _ -> finishAffinity() }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    override fun onResume() {
        super.onResume()
        updateCartInfo()
    }
}