package com.example.marketplace

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository class for Firebase operations
 */
class FirebaseRepository(private val context: Context? = null) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationHelper: NotificationHelper? = context?.let { NotificationHelper(it) }

    /**
     * Add a new product to Firestore
     */
    suspend fun addProduct(product: Product): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            val productId = System.currentTimeMillis().toString()

            val productData = hashMapOf(
                "productId" to productId,
                "name" to product.name,
                "price" to product.price,
                "description" to product.description,
                "quantity" to product.quantity,
                "imageUrl" to product.imageUrl,
                "sellerCompany" to product.sellerCompany,
                "sellerId" to currentUser.uid,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            // Add product to Firestore
            db.collection("products").document(productId).set(productData).await()

            // Log success for debugging
            Log.d("FirebaseRepository", "Product added successfully: ${product.name}")
            true
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error adding product: ${e.message}", e)
            false
        }
    }

    /**
     * Update existing product
     */
    suspend fun updateProduct(product: Product): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false

            val productData = hashMapOf(
                "name" to product.name,
                "price" to product.price,
                "description" to product.description,
                "quantity" to product.quantity,
                "imageUrl" to product.imageUrl,
                "sellerCompany" to product.sellerCompany,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            db.collection("products").document(product.firebaseProductId)
                .update(productData).await()

            // Check if product is low stock after update
            if (product.quantity <= 10 && product.quantity > 0) {
                notificationHelper?.showLowStockNotification(
                    product.name,
                    product.quantity,
                    product.id
                )
            }

            Log.d("FirebaseRepository", "Product updated successfully: ${product.name}")
            true
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error updating product: ${e.message}", e)
            false
        }
    }

    /**
     * Delete product
     */
    suspend fun deleteProduct(firebaseProductId: String): Boolean {
        return try {
            db.collection("products").document(firebaseProductId).delete().await()
            Log.d("FirebaseRepository", "Product deleted successfully: $firebaseProductId")
            true
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error deleting product: ${e.message}", e)
            false
        }
    }

    /**
     * Get all products
     */
    suspend fun getAllProducts(): List<Product> {
        return try {
            val result = db.collection("products").get().await()
            val products = result.documents.mapNotNull { document ->
                createProductFromDocument(document.data, document.id)
            }
            Log.d("FirebaseRepository", "Loaded ${products.size} products")
            products
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error loading all products: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get products by current seller
     */
    suspend fun getProductsBySeller(): List<Product> {
        return try {
            val currentUser = auth.currentUser ?: return emptyList()
            val result = db.collection("products")
                .whereEqualTo("sellerId", currentUser.uid)
                .get().await()

            val products = result.documents.mapNotNull { document ->
                createProductFromDocument(document.data, document.id)
            }

            Log.d("FirebaseRepository", "Loaded ${products.size} products for seller")

            // Check for low stock products when seller loads their products
            notificationHelper?.checkAndNotifyLowStock(products)

            products
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error loading seller products: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Reduce product quantity after purchase
     */
    suspend fun reduceProductQuantity(firebaseProductId: String, quantityToBuy: Int): Boolean {
        return try {
            val docRef = db.collection("products").document(firebaseProductId)
            val document = docRef.get().await()

            val currentQuantity = document.getLong("quantity")?.toInt() ?: 0
            if (currentQuantity < quantityToBuy) return false

            val newQuantity = currentQuantity - quantityToBuy
            docRef.update("quantity", newQuantity).await()

            // Check if this purchase made the product low stock
            if (newQuantity <= 10 && newQuantity > 0 && currentQuantity > 10) {
                // Get product details for notification
                val productData = document.data
                val product = createProductFromDocument(productData, firebaseProductId)

                if (product != null) {
                    // Get the seller ID to check if we should notify
                    val sellerId = productData?.get("sellerId") as? String

                    // Only notify if this is the seller's product
                    if (sellerId == auth.currentUser?.uid) {
                        notificationHelper?.showLowStockNotification(
                            product.name,
                            newQuantity,
                            product.id
                        )
                    }
                }
            }

            true
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error reducing quantity: ${e.message}", e)
            false
        }
    }

    /**
     * Process purchase (reduce quantity and add to history)
     */
    suspend fun processPurchase(purchase: Purchase, firebaseProductId: String): Boolean {
        return try {
            val quantityReduced = reduceProductQuantity(firebaseProductId, purchase.quantity)
            if (!quantityReduced) return false

            addPurchase(purchase)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error processing purchase: ${e.message}", e)
            false
        }
    }

    /**
     * Add purchase to history
     */
    suspend fun addPurchase(purchase: Purchase): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false

            val purchaseData = hashMapOf(
                "productId" to purchase.productId,
                "productName" to purchase.productName,
                "price" to purchase.price,
                "quantity" to purchase.quantity,
                "date" to purchase.date,
                "userId" to currentUser.uid,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            db.collection("purchases").add(purchaseData).await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error adding purchase: ${e.message}", e)
            false
        }
    }

    /**
     * Get purchase history for current user
     */
    suspend fun getPurchaseHistory(): List<Purchase> {
        return try {
            val currentUser = auth.currentUser ?: return emptyList()
            val result = db.collection("purchases")
                .whereEqualTo("userId", currentUser.uid)
                .get().await()

            result.documents.mapNotNull { document ->
                Purchase(
                    productId = document.getLong("productId") ?: 0L,
                    productName = document.getString("productName") ?: "",
                    price = document.getDouble("price") ?: 0.0,
                    quantity = document.getLong("quantity")?.toInt() ?: 0,
                    date = document.getString("date") ?: getCurrentDate()
                )
            }.sortedByDescending {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date)?.time ?: 0L
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error loading purchase history: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Check if user already reviewed product
     */
    suspend fun hasUserReviewedProduct(productId: Long): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            val result = db.collection("reviews")
                .whereEqualTo("productId", productId)
                .whereEqualTo("userId", currentUser.uid)
                .limit(1)
                .get().await()
            !result.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Add review for product
     */
    suspend fun addReview(review: Review): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false

            if (hasUserReviewedProduct(review.productId)) return false

            val reviewData = hashMapOf(
                "productId" to review.productId,
                "userId" to currentUser.uid,
                "username" to review.username,
                "rating" to review.rating,
                "comment" to review.comment,
                "date" to getCurrentDate(),
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            db.collection("reviews").add(reviewData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get reviews for specific product
     */
    suspend fun getReviewsForProduct(productId: Long): List<Review> {
        return try {
            val result = db.collection("reviews")
                .whereEqualTo("productId", productId)
                .get().await()

            result.documents.mapNotNull { document ->
                Review(
                    productId = document.getLong("productId") ?: 0L,
                    username = document.getString("username") ?: "Anonymous",
                    rating = document.getDouble("rating")?.toFloat() ?: 0f,
                    comment = document.getString("comment") ?: "",
                    date = document.getString("date") ?: getCurrentDate()
                )
            }.sortedByDescending {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date)?.time ?: 0L
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Check if user purchased specific product
     */
    suspend fun hasUserPurchasedProduct(productId: Long): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            val result = db.collection("purchases")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("productId", productId)
                .limit(1)
                .get().await()
            !result.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get current user info
     */
    suspend fun getCurrentUserInfo(): Map<String, Any>? {
        return try {
            val currentUser = auth.currentUser ?: return null
            val document = db.collection("users").document(currentUser.uid).get().await()
            document.data
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Helper function to create Product from Firestore document
     */
    private fun createProductFromDocument(data: Map<String, Any>?, documentId: String): Product? {
        if (data == null) return null

        val firebaseProductId = data["productId"] as? String ?: documentId
        return Product(
            id = firebaseProductId.hashCode().toLong(),
            name = data["name"] as? String ?: "",
            price = data["price"] as? Double ?: 0.0,
            description = data["description"] as? String ?: "",
            quantity = (data["quantity"] as? Long)?.toInt() ?: 0,
            imageUrl = data["imageUrl"] as? String ?: "",
            sellerId = data["sellerId"] as? String ?: "",
            sellerCompany = data["sellerCompany"] as? String ?: "Unknown Company",
            firebaseProductId = firebaseProductId
        )
    }

    /**
     * Get current date string
     */
    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}