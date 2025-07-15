package com.example.fastfood.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fastfood.R
import com.example.fastfood.databinding.ItemOrderDetailBinding
import com.example.fastfood.models.OrderItem
import java.text.NumberFormat
import java.util.*

class OrderItemAdapter : ListAdapter<OrderItem, OrderItemAdapter.OrderItemViewHolder>(OrderItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
        val binding = ItemOrderDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OrderItemViewHolder(
        private val binding: ItemOrderDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val numberFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        fun bind(orderItem: OrderItem) {
            binding.apply {
                // Product info
                tvProductName.text = orderItem.foodName
                tvProductDescription.text = "" // OrderItem doesn't have description
                tvQuantity.text = "x${orderItem.quantity}"
                tvPrice.text = "${numberFormat.format(orderItem.price)}₫"
                tvTotalPrice.text = "${numberFormat.format(orderItem.quantity * orderItem.price)}₫"

                // Product image - use placeholder since OrderItem doesn't have image
                ivProductImage.setImageResource(R.drawable.placeholder_food)

                // Notes if any
                if (!orderItem.note.isNullOrBlank()) {
                    tvItemNotes.text = "Ghi chú: ${orderItem.note}"
                    tvItemNotes.visibility = android.view.View.VISIBLE
                } else {
                    tvItemNotes.visibility = android.view.View.GONE
                }

                // Hide description if empty
                tvProductDescription.visibility = android.view.View.GONE
            }
        }
    }

    class OrderItemDiffCallback : DiffUtil.ItemCallback<OrderItem>() {
        override fun areItemsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean {
            return oldItem.foodId == newItem.foodId
        }

        override fun areContentsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean {
            return oldItem == newItem
        }
    }
}
