package com.example.marketplace

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.app.Activity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for displaying user's purchase history
 */
class HistoryActivity : Activity() {

    private lateinit var recyclerViewHistory: RecyclerView
    private lateinit var textViewNoHistory: TextView
    private lateinit var buttonMenu: Button
    private lateinit var buttonBack: Button
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var historyAdapter: HistoryAdapter

    private val purchaseList = mutableListOf<Purchase>()
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        firebaseRepository = FirebaseRepository(this)
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            Toast.makeText(this, "Please log in to view purchase history", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        loadPurchaseHistory()
        setupClickListeners()
    }

    /**
     * Initialize UI components
     */
    private fun initViews() {
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory)
        textViewNoHistory = findViewById(R.id.textViewNoHistory)
        buttonMenu = findViewById(R.id.buttonMenu)
        buttonBack = findViewById(R.id.buttonBack)

        recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        historyAdapter = HistoryAdapter(purchaseList, this)
        recyclerViewHistory.adapter = historyAdapter

        textViewNoHistory.text = "No purchase history to display.\nMake some purchases to see them here!"
    }

    /**
     * Setup button click listeners
     */
    private fun setupClickListeners() {
        buttonMenu.setOnClickListener { showMenu() }
        buttonBack.setOnClickListener { finish() }
    }

    /**
     * Show simple menu dialog
     */
    private fun showMenu() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("History Menu")
        builder.setMessage("Additional features coming soon!")
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    /**
     * Load purchase history from database
     */
    private fun loadPurchaseHistory() {
        if (isLoading) return

        isLoading = true
        showLoadingState()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val purchases = firebaseRepository.getPurchaseHistory()

                withContext(Dispatchers.Main) {
                    isLoading = false
                    hideLoadingState()

                    purchaseList.clear()
                    purchaseList.addAll(purchases)
                    historyAdapter.notifyDataSetChanged()
                    updateHistoryDisplay()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    hideLoadingState()
                    Toast.makeText(this@HistoryActivity,
                        "Error loading purchase history: ${e.message}",
                        Toast.LENGTH_LONG).show()
                    updateHistoryDisplay()
                }
            }
        }
    }

    /**
     * Update history display based on list content
     */
    private fun updateHistoryDisplay() {
        if (purchaseList.isEmpty()) {
            textViewNoHistory.visibility = View.VISIBLE
            recyclerViewHistory.visibility = View.GONE
        } else {
            textViewNoHistory.visibility = View.GONE
            recyclerViewHistory.visibility = View.VISIBLE
        }
    }

    /**
     * Show loading state
     */
    private fun showLoadingState() {
        if (purchaseList.isEmpty()) {
            textViewNoHistory.text = "Loading purchase history..."
            textViewNoHistory.visibility = View.VISIBLE
            recyclerViewHistory.visibility = View.GONE
        }
    }

    /**
     * Hide loading state
     */
    private fun hideLoadingState() {
        textViewNoHistory.text = "No purchase history to display.\nMake some purchases to see them here!"
    }

    override fun onResume() {
        super.onResume()
        loadPurchaseHistory()
    }

    override fun onBackPressed() {
        finish()
    }
}