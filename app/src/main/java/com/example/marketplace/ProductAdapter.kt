package com.example.marketplace

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for product listings
 * @param isSellerView true for seller's product management, false for buyer browsing
 */
class ProductAdapter(
    private val productList: List<Product>,
    private val context: Context,
    private val isSellerView: Boolean = false
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardViewProduct: CardView = itemView.findViewById(R.id.cardViewProduct)
        val imageViewProduct: ImageView = itemView.findViewById(R.id.imageViewProduct)
        val textViewProductItemName: TextView = itemView.findViewById(R.id.textViewProductItemName)
        val textViewCompanyInfo: TextView = itemView.findViewById(R.id.textViewCompanyInfo)
        val textViewQuantityAvailable: TextView = itemView.findViewById(R.id.textViewQuantityAvailable)
        val textViewProductItemPrice: TextView = itemView.findViewById(R.id.textViewProductItemPrice)
        val buttonAddToCart: Button = itemView.findViewById(R.id.buttonAddToCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        holder.textViewProductItemName.text = product.name
        holder.textViewCompanyInfo.text = "Company: ${product.sellerCompany}"
        holder.textViewQuantityAvailable.text = "${product.quantity} Pieces Available"
        holder.textViewProductItemPrice.text = String.format("%.2f USD", product.price)

        loadProductImage(holder.imageViewProduct, product.imageUrl)
        setupButton(holder.buttonAddToCart, product)
        setupCardClick(holder.cardViewProduct, product)
    }

    override fun getItemCount(): Int = productList.size

    private fun loadProductImage(imageView: ImageView, imageUrl: String?) {
        imageView.setImageResource(R.drawable.ic_launcher_foreground)
    }

    /**
     * Configures button text and behavior based on context (seller management vs buyer shopping)
     */
    private fun setupButton(button: Button, product: Product) {
        if (isSellerView) {
            button.text = "EDIT"
            button.setOnClickListener { openEditActivity(product) }
        } else {
            val userType = getUserType()
            if (userType == "seller") {
                button.text = "Sellers Can't Buy"
                button.isEnabled = false
                button.setOnClickListener { }
            } else {
                button.text = "Add To Cart"
                button.isEnabled = product.quantity > 0
                button.setOnClickListener { addToCart(product) }
            }
        }
    }

    private fun getUserType(): String {
        val sharedPreferences = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return sharedPreferences.getString("user_type", "buyer") ?: "buyer"
    }

    private fun setupCardClick(cardView: CardView, product: Product) {
        cardView.setOnClickListener {
            val intent = Intent(context, ProductActivity::class.java).apply {
                putExtra("product_id", product.id)
                putExtra("product_name", product.name)
                putExtra("product_price", product.price)
                putExtra("product_description", product.description)
                putExtra("product_quantity", product.quantity)
                putExtra("product_image_url", product.imageUrl)
                putExtra("product_seller_company", product.sellerCompany)
                putExtra("firebase_product_id", product.firebaseProductId)
            }
            context.startActivity(intent)
        }
    }

    private fun openEditActivity(product: Product) {
        if (product.firebaseProductId.isEmpty()) {
            return
        }

        val intent = Intent(context, EditProductActivity::class.java).apply {
            putExtra("product_id", product.id)
            putExtra("firebase_product_id", product.firebaseProductId)
            putExtra("product_name", product.name)
            putExtra("product_price", product.price)
            putExtra("product_description", product.description)
            putExtra("product_quantity", product.quantity)
            putExtra("product_company", product.sellerCompany)
            putExtra("product_image_url", product.imageUrl)
        }

        if (context is android.app.Activity) {
            context.startActivityForResult(intent, 1001)
        } else {
            context.startActivity(intent)
        }
    }

    /**
     * Adds product to cart while respecting available quantity limits
     */
    private fun addToCart(product: Product) {
        val userType = getUserType()
        if (userType == "seller") {
            return
        }

        if (product.quantity <= 0) {
            return
        }

        if (product.firebaseProductId.isEmpty()) {
            return
        }

        val sharedPreferences = context.getSharedPreferences("cart_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val currentQuantityInCart = sharedPreferences.getInt("product_${product.id}_quantity", 0)

        if (currentQuantityInCart >= product.quantity) {
            Toast.makeText(context,
                "Cannot add more. Maximum available: ${product.quantity}",
                Toast.LENGTH_LONG).show()
            return
        }

        val newQuantity = currentQuantityInCart + 1

        editor.putString("product_${product.id}_name", product.name)
        editor.putFloat("product_${product.id}_price", product.price.toFloat())
        editor.putString("product_${product.id}_image", product.imageUrl)
        editor.putInt("product_${product.id}_quantity", newQuantity)
        editor.putString("product_${product.id}_firebase_id", product.firebaseProductId)
        editor.putInt("product_${product.id}_max_quantity", product.quantity)

        val productsInCart = sharedPreferences.getStringSet("cart_product_ids", mutableSetOf()) ?: mutableSetOf()
        productsInCart.add(product.id.toString())
        editor.putStringSet("cart_product_ids", productsInCart)

        editor.apply()
    }
}