package com.example.marketplace

/**
 * Data class representing an item in the shopping cart
 *
 * @property productId Local product ID
 * @property productName Name of the product
 * @property price Price per unit
 * @property quantity Quantity in cart
 * @property imageUrl Product image URL
 * @property firebaseProductId Firebase document ID for the product
 */
data class CartItem(
    val productId: Long,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String = "",
    val firebaseProductId: String = ""
)