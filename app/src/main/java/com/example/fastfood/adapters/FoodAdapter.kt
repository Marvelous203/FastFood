package com.example.fastfood.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fastfood.R
import com.example.fastfood.databinding.ItemFoodBinding
import com.example.fastfood.models.Food
import java.text.NumberFormat
import java.util.Locale

class FoodAdapter(
    private val foods: List<Food>,
    private val onItemClick: (Food) -> Unit,
    private val onAddToCartClick: (Food) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    inner class FoodViewHolder(
        private val binding: ItemFoodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(food: Food) {
            with(binding) {
                // Load food image
                Glide.with(foodImage)
                    .load(food.images.firstOrNull()?.path)
                    .placeholder(R.drawable.placeholder_food)
                    .error(R.drawable.placeholder_food)
                    .centerCrop()
                    .into(foodImage)

                // Set food name and brand
                foodName.text = food.name
                brandName.text = food.brand ?: ""
                brandName.visibility = if (food.brand.isNullOrEmpty()) View.GONE else View.VISIBLE

                // Handle discount and prices
                val hasDiscount = food.discount > 0
                if (hasDiscount) {
                    discountBadge.text = "-${food.discount}%"
                    discountBadge.visibility = View.VISIBLE
                    
                    originalPrice.text = formatPrice(food.price)
                    originalPrice.paintFlags = originalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    originalPrice.visibility = View.VISIBLE
                    
                    val discountedPrice = food.price * (1 - food.discount / 100.0)
                    price.text = formatPrice(discountedPrice)
                } else {
                    discountBadge.visibility = View.GONE
                    originalPrice.visibility = View.GONE
                    price.text = formatPrice(food.price)
                }

                // Handle stock status
                when {
                    food.stock == 0 -> {
                        stockStatus.text = root.context.getString(R.string.stock_out)
                        stockStatus.visibility = View.VISIBLE
                        addToCartButton.isEnabled = false
                    }
                    food.stock <= 5 -> {
                        stockStatus.text = root.context.getString(R.string.stock_limited)
                        stockStatus.visibility = View.VISIBLE
                        addToCartButton.isEnabled = true
                    }
                    else -> {
                        stockStatus.visibility = View.GONE
                        addToCartButton.isEnabled = true
                    }
                }

                // Handle diet type badge
                dietBadge.visibility = when {
                    food.isVegan -> {
                        dietBadge.setImageResource(R.drawable.ic_vegan)
                        dietBadge.contentDescription = root.context.getString(R.string.vegan)
                        View.VISIBLE
                    }
                    food.isVegetarian -> {
                        dietBadge.setImageResource(R.drawable.ic_vegetarian)
                        dietBadge.contentDescription = root.context.getString(R.string.vegetarian)
                        View.VISIBLE
                    }
                    else -> View.GONE
                }

                // Set rating
                if (food.rating > 0) {
                    rating.text = String.format("%.1f", food.rating)
                    val ratingContainer = (rating.parent as ViewGroup).parent as View
                    ratingContainer.visibility = View.VISIBLE
                } else {
                    val ratingContainer = (rating.parent as ViewGroup).parent as View
                    ratingContainer.visibility = View.GONE
                }

                // Set click listeners
                root.setOnClickListener { onItemClick(food) }
                addToCartButton.setOnClickListener { onAddToCartClick(food) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(foods[position])
    }

    override fun getItemCount() = foods.size

    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return formatter.format(price).replace("₫", "đ")
    }
} 