package com.example.marketplace

/**
 * Data class representing a completed purchase
 *
 * @property productId ID of purchased product
 * @property productName Name of purchased product
 * @property price Price paid
 * @property quantity Quantity purchased
 * @property date Purchase date in YYYY-MM-DD format
 */
data class Purchase(
    val productId: Long,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val date: String
)