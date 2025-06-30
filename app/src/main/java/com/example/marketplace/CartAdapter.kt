package com.example.marketplace

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for cart items
 */
class CartAdapter(
    private val cartItems: MutableList<CartItem>,
    private val context: Context,
    private val onCartUpdated: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    /**
     * ViewHolder for cart items
     */
    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewCartProduct: ImageView = itemView.findViewById(R.id.imageViewCartProduct)
        val textViewCartProductName: TextView = itemView.findViewById(R.id.textViewCartProductName)
        val textViewCartProductPrice: TextView = itemView.findViewById(R.id.textViewCartProductPrice)
        val textViewCartQuantity: TextView = itemView.findViewById(R.id.textViewCartQuantity)
        val buttonDecreaseQuantity: Button = itemView.findViewById(R.id.buttonDecreaseQuantity)
        val buttonIncreaseQuantity: Button = itemView.findViewById(R.id.buttonIncreaseQuantity)
        val buttonRemoveFromCart: Button = itemView.findViewById(R.id.buttonRemoveFromCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cart_item, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartItem = cartItems[position]

        holder.textViewCartProductName.text = cartItem.productName
        holder.textViewCartProductPrice.text = String.format("%.2f USD", cartItem.price)
        holder.textViewCartQuantity.text = cartItem.quantity.toString()

        loadItemImage(holder.imageViewCartProduct, cartItem.imageUrl)
        setupQuantityButtons(holder, cartItem, position)
        setupRemoveButton(holder.buttonRemoveFromCart, cartItem, position)
    }

    override fun getItemCount(): Int = cartItems.size

    /**
     * Load cart item image (always use placeholder)
     * @param imageView ImageView to load into
     * @param imageUrl Image URL (not used)
     */
    private fun loadItemImage(imageView: ImageView, imageUrl: String?) {
        // Always use placeholder image
        imageView.setImageResource(R.drawable.ic_launcher_foreground)
    }

    /**
     * Setup quantity increase/decrease buttons with validation
     * @param holder ViewHolder
     * @param cartItem Cart item data
     * @param position Item position
     */
    private fun setupQuantityButtons(holder: CartViewHolder, cartItem: CartItem, position: Int) {
        // Decrease button
        holder.buttonDecreaseQuantity.setOnClickListener {
            if (cartItem.quantity > 1) {
                updateCartItemQuantity(cartItem.productId, cartItem.quantity - 1, position)
            }
        }

        // Increase button
        holder.buttonIncreaseQuantity.setOnClickListener {
            // Get max available quantity from shared preferences
            val sharedPreferences = context.getSharedPreferences("cart_data", Context.MODE_PRIVATE)
            val maxQuantity = sharedPreferences.getInt("product_${cartItem.productId}_max_quantity", cartItem.quantity)

            if (cartItem.quantity < maxQuantity) {
                updateCartItemQuantity(cartItem.productId, cartItem.quantity + 1, position)
            } else {
                Toast.makeText(context,
                    "Cannot add more. Maximum available: $maxQuantity",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Setup remove button with confirmation dialog
     * @param button Remove button
     * @param cartItem Cart item data
     * @param position Item position
     */
    private fun setupRemoveButton(button: Button, cartItem: CartItem, position: Int) {
        button.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(context)
            builder.setTitle("Remove Item")
            builder.setMessage("Remove '${cartItem.productName}' from cart?")
            builder.setPositiveButton("Yes") { _, _ ->
                removeFromCart(cartItem.productId, position)
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }
    }

    /**
     * Update cart item quantity with validation
     * @param productId Product ID
     * @param newQuantity New quantity
     * @param position Item position
     */
    private fun updateCartItemQuantity(productId: Long, newQuantity: Int, position: Int) {
        val sharedPreferences = context.getSharedPreferences("cart_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putInt("product_${productId}_quantity", newQuantity)
        editor.apply()

        val oldItem = cartItems[position]
        cartItems[position] = CartItem(
            productId = oldItem.productId,
            productName = oldItem.productName,
            price = oldItem.price,
            quantity = newQuantity,
            imageUrl = oldItem.imageUrl,
            firebaseProductId = oldItem.firebaseProductId
        )

        notifyItemChanged(position)
        onCartUpdated()
    }

    /**
     * Remove item from cart
     * @param productId Product ID to remove
     * @param position Item position
     */
    private fun removeFromCart(productId: Long, position: Int) {
        val sharedPreferences = context.getSharedPreferences("cart_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Remove all product data
        editor.remove("product_${productId}_name")
        editor.remove("product_${productId}_price")
        editor.remove("product_${productId}_quantity")
        editor.remove("product_${productId}_image")
        editor.remove("product_${productId}_firebase_id")
        editor.remove("product_${productId}_max_quantity")

        // Update product IDs set
        val productIds = sharedPreferences.getStringSet("cart_product_ids", mutableSetOf())?.toMutableSet()
        productIds?.remove(productId.toString())
        editor.putStringSet("cart_product_ids", productIds)

        editor.apply()

        // Update UI
        cartItems.removeAt(position)
        notifyItemRemoved(position)
        onCartUpdated()
    }
}