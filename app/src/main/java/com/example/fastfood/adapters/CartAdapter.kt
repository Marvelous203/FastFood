package com.example.fastfood.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fastfood.R
import com.example.fastfood.models.CartItem
import com.example.fastfood.models.Food
import kotlinx.coroutines.*

class CartAdapter(
    private val onQuantityChanged: suspend (String, Int) -> Boolean, // Changed to return Boolean for success status
    private val onRemoveClick: (String) -> Unit,
    private val getProductDetails: suspend (String) -> Food?
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    private val adapterScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foodImage: ImageView = itemView.findViewById(R.id.food_image)
        private val foodName: TextView = itemView.findViewById(R.id.food_name)
        private val foodPrice: TextView = itemView.findViewById(R.id.food_price)
        private val quantity: TextView = itemView.findViewById(R.id.quantity)
        private val btnDecrease: ImageButton = itemView.findViewById(R.id.btn_decrease)
        private val btnIncrease: ImageButton = itemView.findViewById(R.id.btn_increase)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btn_remove)
        private var currentItem: CartItem? = null
        private var updateJob: Job? = null

        fun bind(item: CartItem) {
            currentItem = item
            // Set quantity immediately
            updateQuantityDisplay(item.quantity)

            // Sử dụng thông tin đã có sẵn trong CartItem
            if (item.name.isNotEmpty() && item.price > 0) {
                // Có thông tin sản phẩm trong cache
                foodName.text = item.name
                updatePriceDisplay(item.price, item.quantity)

                // Load image
                if (item.image.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(item.image)
                        .placeholder(R.drawable.placeholder_food)
                        .error(R.drawable.placeholder_food)
                        .into(foodImage)
                } else {
                    Glide.with(itemView.context)
                        .load(R.drawable.placeholder_food)
                        .into(foodImage)
                }
            } else {
                // Chưa có thông tin sản phẩm, fetch từ API
                foodName.text = "Đang tải..."
                foodPrice.text = ""
                Glide.with(itemView.context)
                    .load(R.drawable.placeholder_food)
                    .into(foodImage)

                // Cancel any existing update job
                updateJob?.cancel()

                // Fetch product details
                adapterScope.launch {
                    try {
                        val product = withContext(Dispatchers.IO) {
                            getProductDetails(item.foodId)
                        }

                        if (product != null) {
                            foodName.text = product.name
                            val finalPrice = if (product.discount > 0) {
                                product.price * (1 - product.discount / 100.0)
                            } else {
                                product.price
                            }
                            updatePriceDisplay(finalPrice, item.quantity)

                            // Load first product image if available
                            if (!product.images.isNullOrEmpty()) {
                            try {
                                Glide.with(itemView.context)
                                    .load(product.images[0].path)
                                    .placeholder(R.drawable.placeholder_food)
                                    .error(R.drawable.placeholder_food)
                                    .timeout(10000) // 10 second timeout
                                    .into(foodImage)
                            } catch (e: Exception) {
                                // Fallback to placeholder if Glide fails
                                Glide.with(itemView.context)
                                    .load(R.drawable.placeholder_food)
                                    .into(foodImage)
                            }
                        }
                    } else {
                        foodName.text = "Sản phẩm không tồn tại"
                        foodPrice.text = ""
                    }
                } catch (e: Exception) {
                    foodName.text = "Lỗi tải thông tin"
                    foodPrice.text = ""
                }
            }
            }

            // Handle quantity changes with debounce
            btnDecrease.setOnClickListener {
                if (item.quantity > 1) {
                    updateQuantity(item.quantity - 1)
                }
            }

            btnIncrease.setOnClickListener {
                updateQuantity(item.quantity + 1)
            }

            btnRemove.setOnClickListener {
                onRemoveClick(item.foodId)
            }
        }

        private fun updateQuantityDisplay(newQuantity: Int) {
            quantity.text = newQuantity.toString()
        }

        private fun updatePriceDisplay(unitPrice: Double, quantity: Int) {
            foodPrice.text = itemView.context.getString(
                R.string.price_format,
                unitPrice * quantity
            )
        }

        private fun updateQuantity(newQuantity: Int) {
            val item = currentItem ?: return

            // Update UI immediately
            updateQuantityDisplay(newQuantity)

            // Cancel previous update if any
            updateJob?.cancel()

            // Start new update with delay
            updateJob = adapterScope.launch {
                delay(500) // Debounce for 500ms
                try {
                    val success = onQuantityChanged(item.foodId, newQuantity)
                    if (!success) {
                        // If update failed, revert to original quantity
                        updateQuantityDisplay(item.quantity)
                    }
                } catch (e: Exception) {
                    // If update failed, revert to original quantity
                    updateQuantityDisplay(item.quantity)
                }
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapterScope.cancel() // Clean up coroutines when adapter is detached
    }

    private class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.foodId == newItem.foodId
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}
