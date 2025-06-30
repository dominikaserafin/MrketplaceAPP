package com.example.marketplace

/**
 * Data class representing a product in the marketplace
 *
 * @property id Local product ID
 * @property name Product name
 * @property price Product price in USD
 * @property description Product description
 * @property quantity Available quantity
 * @property imageUrl URL to product image
 * @property sellerId Firebase user ID of seller
 * @property sellerCompany Company name of seller
 * @property firebaseProductId Firebase document ID
 */
data class Product(
    var id: Long = 0,
    var name: String = "",
    var price: Double = 0.0,
    var description: String = "",
    var quantity: Int = 0,
    var imageUrl: String = "",
    var sellerId: String = "",
    var sellerCompany: String = "",
    var firebaseProductId: String = ""
)