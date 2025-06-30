package com.example.marketplace

import android.content.Intent
import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for browsing and searching products
 */
class BrowserActivity : Activity() {

    private lateinit var editTextSearch: EditText
    private lateinit var recyclerViewProducts: RecyclerView
    private lateinit var textViewNoProducts: TextView
    private lateinit var buttonMenu: Button
    private lateinit var buttonCart: Button
    private lateinit var buttonBack: Button
    private lateinit var textViewSearchInfo: TextView
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var productAdapter: ProductAdapter

    private val allProductList = mutableListOf<Product>()
    private val filteredProductList = mutableListOf<Product>()
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser_enhanced)

        firebaseRepository = FirebaseRepository(this)

        initViews()
        hideCartForSellers()
        loadProducts()
        setupSearch()
        setupClickListeners()
    }

    /**
     * Initialize UI components
     */
    private fun initViews() {
        editTextSearch = findViewById(R.id.editTextSearch)
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts)
        textViewNoProducts = findViewById(R.id.textViewNoProducts)
        buttonMenu = findViewById(R.id.buttonMenu)
        buttonCart = findViewById(R.id.buttonCart)
        buttonBack = findViewById(R.id.buttonBack)
        textViewSearchInfo = findViewById(R.id.textViewSearchInfo)

        recyclerViewProducts.layoutManager = LinearLayoutManager(this)
        productAdapter = ProductAdapter(filteredProductList, this, false)
        recyclerViewProducts.adapter = productAdapter
    }

    /**
     * Hide cart button for sellers
     */
    private fun hideCartForSellers() {
        val sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE)
        val userType = sharedPreferences.getString("user_type", "buyer")

        if (userType == "seller") {
            buttonCart.visibility = View.GONE
        }
    }

    /**
     * Setup button click listeners
     */
    private fun setupClickListeners() {
        buttonMenu.setOnClickListener { showMenu() }
        buttonCart.setOnClickListener {
            val userType = getSharedPreferences("user_data", MODE_PRIVATE)
                .getString("user_type", "buyer")

            if (userType == "buyer") {
                startActivity(Intent(this, CartActivity::class.java))
            }
        }
        buttonBack.setOnClickListener { finish() }
    }

    /**
     * Show simple menu dialog
     */
    private fun showMenu() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Browser Menu")
        builder.setMessage("Additional features coming soon!")
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    /**
     * Load all products from database
     */
    private fun loadProducts() {
        if (isLoading) return

        isLoading = true
        showLoadingState()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val products = firebaseRepository.getAllProducts()

                withContext(Dispatchers.Main) {
                    isLoading = false
                    hideLoadingState()

                    allProductList.clear()
                    allProductList.addAll(products)

                    filteredProductList.clear()
                    filteredProductList.addAll(products)

                    productAdapter.notifyDataSetChanged()
                    updateProductsDisplay()
                    updateSearchInfo("")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    hideLoadingState()
                    Toast.makeText(this@BrowserActivity,
                        "Error loading products: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Setup search functionality
     */
    private fun setupSearch() {
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                filterProducts(query)
                updateSearchInfo(query)
            }
        })
    }

    /**
     * Filter products based on search query
     */
    private fun filterProducts(query: String) {
        filteredProductList.clear()

        if (query.trim().isEmpty()) {
            filteredProductList.addAll(allProductList)
        } else {
            val searchQuery = query.trim().lowercase()

            for (product in allProductList) {
                val matchesName = product.name.lowercase().contains(searchQuery)
                val matchesDescription = product.description.lowercase().contains(searchQuery)
                val matchesCompany = product.sellerCompany.lowercase().contains(searchQuery)

                if (matchesName || matchesDescription || matchesCompany) {
                    filteredProductList.add(product)
                }
            }
        }

        productAdapter.notifyDataSetChanged()
        updateProductsDisplay()
    }

    /**
     * Update search information display
     */
    private fun updateSearchInfo(query: String) {
        if (query.trim().isEmpty()) {
            textViewSearchInfo.text = "Showing all ${allProductList.size} products"
            textViewSearchInfo.visibility = View.VISIBLE
        } else {
            val resultsText = when (filteredProductList.size) {
                0 -> "No products found for '$query'"
                1 -> "Found 1 product for '$query'"
                else -> "Found ${filteredProductList.size} products for '$query'"
            }
            textViewSearchInfo.text = resultsText
            textViewSearchInfo.visibility = View.VISIBLE
        }
    }

    /**
     * Update products display based on list content
     */
    private fun updateProductsDisplay() {
        if (filteredProductList.isEmpty()) {
            recyclerViewProducts.visibility = View.GONE
            textViewNoProducts.visibility = View.VISIBLE
        } else {
            recyclerViewProducts.visibility = View.VISIBLE
            textViewNoProducts.visibility = View.GONE
        }
    }

    /**
     * Show loading state
     */
    private fun showLoadingState() {
        if (filteredProductList.isEmpty()) {
            textViewNoProducts.text = "Loading products..."
            textViewNoProducts.visibility = View.VISIBLE
            recyclerViewProducts.visibility = View.GONE
            textViewSearchInfo.visibility = View.GONE
        }
    }

    /**
     * Hide loading state
     */
    private fun hideLoadingState() {
        textViewNoProducts.text = "No products found"
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }

    override fun onBackPressed() {
        finish()
    }
}