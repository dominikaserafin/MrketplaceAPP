package com.example.marketplace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for displaying product reviews
 */
class ReviewAdapter(
    private val reviewList: List<Review>
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewReviewUsername: TextView = itemView.findViewById(R.id.textViewReviewUsername)
        val textViewReviewDate: TextView = itemView.findViewById(R.id.textViewReviewDate)
        val textViewReviewComment: TextView = itemView.findViewById(R.id.textViewReviewComment)
        val ratingBarReviewItem: RatingBar = itemView.findViewById(R.id.ratingBarReviewItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.review_item, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviewList[position]

        holder.textViewReviewUsername.text = review.username
        holder.ratingBarReviewItem.rating = review.rating
        holder.textViewReviewDate.text = review.date
        holder.textViewReviewComment.text = review.comment
    }

    override fun getItemCount(): Int = reviewList.size
}