package com.example.marketplace

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main screen for sellers to manage products and receive low stock notifications
 */
class SellerActivity : Activity() {

    private lateinit var buttonAddProduct: Button
    private lateinit var buttonBrowseProducts: Button
    private lateinit var recyclerViewProducts: RecyclerView
    private lateinit var textViewNoProducts: TextView
    private lateinit var buttonMenu: Button
    private lateinit var buttonLogout: Button
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var productAdapter: ProductAdapter

    private val productList = mutableListOf<Product>()
    private var isLoading = false

    companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_enhanced)

        firebaseRepository = FirebaseRepository(this)
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            finish()
            return
        }

        saveUserType("seller")
        initViews()
        setupClickListeners()
        checkNotificationPermission()
    }

    /**
     * Requests notification permissions on Android 13+ before loading products
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {
                loadSellerProducts()
            }
        } else {
            loadSellerProducts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            loadSellerProducts()
        }
    }

    private fun initViews() {
        buttonAddProduct = findViewById(R.id.buttonAddProduct)
        buttonBrowseProducts = findViewById(R.id.buttonBrowseProducts)
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts)
        textViewNoProducts = findViewById(R.id.textViewNoProducts)
        buttonMenu = findViewById(R.id.buttonMenu)
        buttonLogout = findViewById(R.id.buttonLogout)

        recyclerViewProducts.layoutManager = LinearLayoutManager(this)
        productAdapter = ProductAdapter(productList, this, true)
        recyclerViewProducts.adapter = productAdapter
    }

    private fun setupClickListeners() {
        buttonAddProduct.setOnClickListener {
            startActivityForResult(Intent(this, AddProductActivity::class.java), 1001)
        }

        buttonBrowseProducts.setOnClickListener {
            startActivity(Intent(this, BrowserActivity::class.java))
        }

        buttonMenu.setOnClickListener { showMenu() }
        buttonLogout.setOnClickListener { showLogoutDialog() }
    }

    private fun showMenu() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Seller Menu")
        builder.setMessage("Additional features coming soon!")
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    /**
     * Fetches seller's products and triggers low stock notifications if needed
     */
    private fun loadSellerProducts() {
        if (isLoading) return

        isLoading = true
        showLoadingState()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val products = firebaseRepository.getProductsBySeller()

                withContext(Dispatchers.Main) {
                    isLoading = false
                    hideLoadingState()

                    productList.clear()
                    productList.addAll(products)
                    productAdapter.notifyDataSetChanged()
                    updateProductsDisplay()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    hideLoadingState()
                    updateProductsDisplay()
                }
            }
        }
    }

    /**
     * Loads products with a delay to allow Firebase synchronization
     */
    private fun loadSellerProductsWithDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            loadSellerProducts()
        }, 1000) // 1 second delay for Firebase sync
    }

    private fun updateProductsDisplay() {
        if (productList.isEmpty()) {
            textViewNoProducts.visibility = View.VISIBLE
            recyclerViewProducts.visibility = View.GONE
        } else {
            textViewNoProducts.visibility = View.GONE
            recyclerViewProducts.visibility = View.VISIBLE
        }
    }

    private fun showLoadingState() {
        if (productList.isEmpty()) {
            textViewNoProducts.text = "Loading your products..."
            textViewNoProducts.visibility = View.VISIBLE
            recyclerViewProducts.visibility = View.GONE
        }
    }

    private fun hideLoadingState() {
        textViewNoProducts.text = "No products yet. Add your first product!"
    }

    private fun showLogoutDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Logout Confirmation")
        builder.setMessage("Are you sure you want to logout?")
        builder.setPositiveButton("Yes") { _, _ -> performLogout() }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun performLogout() {
        val notificationHelper = NotificationHelper(this)
        notificationHelper.clearSessionFlag()

        auth.signOut()
        getSharedPreferences("user_data", MODE_PRIVATE).edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

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
        // Load products when returning to screen
        loadSellerProducts()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Use delayed reload to ensure Firebase has time to sync
            loadSellerProductsWithDelay()
        }
    }
}