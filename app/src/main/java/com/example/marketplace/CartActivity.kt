package com.example.marketplace

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shopping cart where buyers can review items before purchase
 */
class CartActivity : Activity() {

    private lateinit var recyclerViewCart: RecyclerView
    private lateinit var textViewCartEmpty: TextView
    private lateinit var textViewTotalPrice: TextView
    private lateinit var buttonCheckout: Button
    private lateinit var buttonMenu: Button
    private lateinit var buttonClearCart: Button
    private lateinit var buttonBack: Button
    private lateinit var cartAdapter: CartAdapter
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var auth: FirebaseAuth

    private val cartItems = mutableListOf<CartItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        firebaseRepository = FirebaseRepository(this)
        auth = FirebaseAuth.getInstance()

        initViews()
        loadCartItems()
        setupClickListeners()
    }

    private fun initViews() {
        recyclerViewCart = findViewById(R.id.recyclerViewCart)
        textViewCartEmpty = findViewById(R.id.textViewCartEmpty)
        textViewTotalPrice = findViewById(R.id.textViewTotalPrice)
        buttonCheckout = findViewById(R.id.buttonCheckout)
        buttonMenu = findViewById(R.id.buttonMenu)
        buttonClearCart = findViewById(R.id.buttonClearCart)
        buttonBack = findViewById(R.id.buttonBack)

        recyclerViewCart.layoutManager = LinearLayoutManager(this)
        cartAdapter = CartAdapter(cartItems, this) {
            updateTotalPrice()
            updateCartDisplay()
        }
        recyclerViewCart.adapter = cartAdapter
    }

    private fun setupClickListeners() {
        buttonMenu.setOnClickListener { showMenu() }
        buttonClearCart.setOnClickListener { clearCart() }
        buttonCheckout.setOnClickListener { checkout() }
        buttonBack.setOnClickListener { finish() }
    }

    private fun showMenu() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Cart Menu")
        builder.setMessage("Additional features coming soon!")
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    /**
     * Loads cart items from local storage
     */
    private fun loadCartItems() {
        val sharedPreferences = getSharedPreferences("cart_data", MODE_PRIVATE)
        val productIds = sharedPreferences.getStringSet("cart_product_ids", emptySet()) ?: emptySet()

        cartItems.clear()

        for (productId in productIds) {
            val name = sharedPreferences.getString("product_${productId}_name", "") ?: ""
            val price = sharedPreferences.getFloat("product_${productId}_price", 0f).toDouble()
            val quantity = sharedPreferences.getInt("product_${productId}_quantity", 0)
            val imageUrl = sharedPreferences.getString("product_${productId}_image", "") ?: ""
            val firebaseId = sharedPreferences.getString("product_${productId}_firebase_id", "") ?: ""

            if (name.isNotEmpty() && quantity > 0 && firebaseId.isNotEmpty()) {
                cartItems.add(CartItem(
                    productId = productId.toLong(),
                    productName = name,
                    price = price,
                    quantity = quantity,
                    imageUrl = imageUrl,
                    firebaseProductId = firebaseId
                ))
            }
        }

        cartAdapter.notifyDataSetChanged()
        updateCartDisplay()
        updateTotalPrice()
    }

    private fun updateCartDisplay() {
        if (cartItems.isEmpty()) {
            textViewCartEmpty.visibility = View.VISIBLE
            recyclerViewCart.visibility = View.GONE
            buttonCheckout.isEnabled = false
            buttonCheckout.text = "Cart Is Empty"
        } else {
            textViewCartEmpty.visibility = View.GONE
            recyclerViewCart.visibility = View.VISIBLE
            buttonCheckout.isEnabled = true

            val itemCount = cartItems.size
            buttonCheckout.text = if (itemCount == 1) {
                "Checkout (1 Item)"
            } else {
                "Checkout ($itemCount Items)"
            }
        }
    }

    private fun updateTotalPrice() {
        val total = cartItems.sumOf { it.price * it.quantity }
        textViewTotalPrice.text = "Total: ${String.format("%.2f", total)} USD"
    }

    private fun clearCart() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Clear Cart")
        builder.setMessage("Are you sure you want to remove all items from your cart?")
        builder.setPositiveButton("Yes") { _, _ ->
            val sharedPreferences = getSharedPreferences("cart_data", MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            cartItems.clear()
            cartAdapter.notifyDataSetChanged()
            updateCartDisplay()
            updateTotalPrice()
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    /**
     * Processes all cart items as purchases and updates inventory
     */
    private fun checkout() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return
        }

        if (cartItems.isEmpty()) {
            return
        }

        val invalidItems = cartItems.filter { it.firebaseProductId.isEmpty() }
        if (invalidItems.isNotEmpty()) {
            return
        }

        buttonCheckout.isEnabled = false
        buttonCheckout.text = "Processing..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var successfulPurchases = 0
                var failedPurchases = 0

                for (item in cartItems) {
                    try {
                        val purchase = Purchase(
                            productId = item.productId,
                            productName = item.productName,
                            price = item.price,
                            quantity = item.quantity,
                            date = getCurrentDate()
                        )

                        val success = firebaseRepository.processPurchase(purchase, item.firebaseProductId)
                        if (success) {
                            successfulPurchases++
                        } else {
                            failedPurchases++
                        }
                    } catch (e: Exception) {
                        failedPurchases++
                    }
                }

                withContext(Dispatchers.Main) {
                    buttonCheckout.isEnabled = true
                    updateCartDisplay()

                    if (successfulPurchases > 0) {
                        val message = if (failedPurchases == 0) {
                            "Purchase successful!"
                        } else {
                            "Purchase completed with some issues"
                        }

                        Toast.makeText(this@CartActivity, message, Toast.LENGTH_LONG).show()

                        if (failedPurchases == 0) {
                            clearCartCompletely()
                            finish()
                        } else {
                            loadCartItems()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    buttonCheckout.isEnabled = true
                    updateCartDisplay()
                }
            }
        }
    }

    private fun clearCartCompletely() {
        getSharedPreferences("cart_data", MODE_PRIVATE).edit().clear().apply()
    }

    private fun getCurrentDate(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date())
    }

    override fun onResume() {
        super.onResume()
        loadCartItems()
    }

    override fun onBackPressed() {
        finish()
    }
}