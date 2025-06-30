package com.example.marketplace

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for displaying purchase history
 */
class HistoryAdapter(
    private val purchaseList: List<Purchase>,
    private val context: Context
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewHistoryProductName: TextView = itemView.findViewById(R.id.textViewHistoryProductName)
        val textViewHistoryDate: TextView = itemView.findViewById(R.id.textViewHistoryDate)
        val textViewHistoryPrice: TextView = itemView.findViewById(R.id.textViewHistoryPrice)
        val textViewHistoryQuantity: TextView = itemView.findViewById(R.id.textViewHistoryQuantity)
        val buttonAddReviewFromHistory: Button = itemView.findViewById(R.id.buttonAddReviewFromHistory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val purchase = purchaseList[position]

        holder.textViewHistoryProductName.text = purchase.productName
        holder.textViewHistoryDate.text = "Purchase date: ${purchase.date}"
        holder.textViewHistoryPrice.text = "Price: $${String.format("%.2f", purchase.price)}"
        holder.textViewHistoryQuantity.text = "Quantity: ${purchase.quantity}"

        holder.buttonAddReviewFromHistory.setOnClickListener {
            if (purchase.productName.isNotEmpty()) {
                val intent = Intent(context, AddReviewActivity::class.java)
                intent.putExtra("product_id", purchase.productId)
                intent.putExtra("product_name", purchase.productName)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Cannot review: Invalid product data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = purchaseList.size
}