package com.example.marketplace

/**
 * Data class representing a product review
 *
 * @property productId ID of reviewed product
 * @property username Name of reviewer
 * @property rating Rating from 1.0 to 5.0
 * @property comment Review comment text
 * @property date Review date in YYYY-MM-DD format
 */
data class Review(
    val productId: Long,
    val username: String,
    val rating: Float,
    val comment: String,
    val date: String
)