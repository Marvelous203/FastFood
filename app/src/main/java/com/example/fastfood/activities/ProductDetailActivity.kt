package com.example.fastfood.activities

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.fastfood.R
import com.example.fastfood.adapters.ImagePagerAdapter
import com.example.fastfood.databinding.ActivityProductDetailBinding
import com.example.fastfood.models.Food
import com.example.fastfood.viewmodels.CartViewModel
import com.example.fastfood.viewmodels.ProductDetailViewModel
import com.example.fastfood.utils.Result
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.example.fastfood.activities.CartActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.fastfood.models.CartItem

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding
    private val productDetailViewModel: ProductDetailViewModel by viewModels()
    private val cartViewModel: CartViewModel by viewModels()
    private val priceFormatter = DecimalFormat("#,###₫")
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var currentFood: Food? = null

    companion object {
        private const val EXTRA_PRODUCT_ID = "product_id"
        private const val MAX_QUANTITY = 10

        fun start(context: Context, productId: String) {
            val intent = Intent(context, ProductDetailActivity::class.java).apply {
                putExtra(EXTRA_PRODUCT_ID, productId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupQuantitySpinner()

        // Get product ID from intent
        val productId = intent.getStringExtra(EXTRA_PRODUCT_ID)
        if (productId != null) {
            productDetailViewModel.loadProduct(productId)
        }

        setupObservers()
        setupClickListeners()
        setupToolbar()
    }

    private fun setupQuantitySpinner() {
        val quantities = (1..MAX_QUANTITY).map { it.toString() }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, quantities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.quantitySpinner.adapter = adapter
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupObservers() {
        productDetailViewModel.product.observe(this) { food ->
            food?.let { 
                currentFood = it
                updateUI(it) 
            }
        }

        productDetailViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        cartViewModel.addToCartResult.observe(this) { result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(this, R.string.added_to_cart_success, Toast.LENGTH_SHORT).show()
                }
                is Result.Failure -> {
                    Toast.makeText(this, result.exception.message ?: getString(R.string.error_add_to_cart), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnAddToCart.setOnClickListener {
            // Show loading state
            binding.btnAddToCart.isEnabled = false
            binding.btnAddToCart.text = "Đang thêm..."

            // Add to cart
            lifecycleScope.launch {
                try {
                    val result = cartViewModel.addToCart(currentFood?.id ?: return@launch, 1)
                    
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is Result.Success -> {
                                // Show success message with action to view cart
                                Snackbar.make(binding.root, "Đã thêm vào giỏ hàng", Snackbar.LENGTH_LONG)
                                    .setAction("Xem giỏ hàng") {
                                        CartActivity.start(this@ProductDetailActivity)
                                    }
                                    .show()
                            }
                            is Result.Failure -> {
                                // Show error message
                                Snackbar.make(binding.root, result.exception.message ?: "Lỗi thêm vào giỏ hàng", Snackbar.LENGTH_LONG)
                                    .show()
                            }
                        }
                        
                        // Reset button state
                        binding.btnAddToCart.isEnabled = true
                        binding.btnAddToCart.text = getString(R.string.add_to_cart)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // Show error message
                        Snackbar.make(binding.root, "Lỗi kết nối: ${e.message}", Snackbar.LENGTH_LONG)
                            .show()
                        
                        // Reset button state
                        binding.btnAddToCart.isEnabled = true
                        binding.btnAddToCart.text = getString(R.string.add_to_cart)
                    }
                }
            }
        }
    }

    private fun updateUI(food: Food) {
        // Setup image slider
        if (food.images.isNotEmpty()) {
            binding.viewPagerImages.apply {
                adapter = ImagePagerAdapter(food.images)
                orientation = ViewPager2.ORIENTATION_HORIZONTAL
                visibility = View.VISIBLE
            }
        } else {
            binding.viewPagerImages.visibility = View.GONE
        }

        // Basic info
        binding.tvFoodName.text = food.name

        // Brand
        if (!food.brand.isNullOrBlank()) {
            binding.tvBrand.apply {
                text = getString(R.string.brand_format, food.brand)
                visibility = View.VISIBLE
            }
        } else {
            binding.tvBrand.visibility = View.GONE
        }

        // Description
        if (!food.description.isNullOrBlank()) {
            binding.tvDescription.apply {
                text = food.description
                visibility = View.VISIBLE
            }
        } else {
            binding.tvDescription.visibility = View.GONE
        }

        // Price and discount
        val finalPrice = if (food.discount > 0) {
            food.price * (1 - food.discount / 100.0)
        } else {
            food.price
        }
        binding.tvPrice.text = priceFormatter.format(finalPrice)

        // Original price and discount badge
        if (food.discount > 0) {
            binding.tvOriginalPrice.apply {
                text = priceFormatter.format(food.price)
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                visibility = View.VISIBLE
            }
            binding.tvDiscountBadge.apply {
                text = getString(R.string.discount_format, food.discount)
                visibility = View.VISIBLE
            }
        } else {
            binding.tvOriginalPrice.visibility = View.GONE
            binding.tvDiscountBadge.visibility = View.GONE
        }

        // Rating
        if (food.rating > 0) {
            binding.tvRating.text = String.format("%.1f", food.rating)
            binding.ratingContainer.visibility = View.VISIBLE
        } else {
            binding.ratingContainer.visibility = View.GONE
        }

        // Product details card
        var hasDetails = false

        // Brand detail
        if (!food.brand.isNullOrBlank()) {
            binding.tvBrandDetail.text = food.brand
            binding.brandRow.visibility = View.VISIBLE
            hasDetails = true
        } else {
            binding.brandRow.visibility = View.GONE
        }

        // Weight
        if (!food.weight.isNullOrBlank()) {
            binding.tvWeight.text = food.weight
            binding.weightRow.visibility = View.VISIBLE
            hasDetails = true
        } else {
            binding.weightRow.visibility = View.GONE
        }

        // Origin
        if (!food.origin.isNullOrBlank()) {
            binding.tvOrigin.text = food.origin
            binding.originRow.visibility = View.VISIBLE
            hasDetails = true
        } else {
            binding.originRow.visibility = View.GONE
        }

        // Packaging
        if (!food.packaging.isNullOrBlank()) {
            binding.tvPackaging.text = food.packaging
            binding.packagingRow.visibility = View.VISIBLE
            hasDetails = true
        } else {
            binding.packagingRow.visibility = View.GONE
        }

        binding.detailsCard.visibility = if (hasDetails) View.VISIBLE else View.GONE

        // Additional information card
        var hasAdditionalInfo = false

        // Ingredients
        if (!food.ingredients.isNullOrBlank()) {
            binding.tvIngredients.text = food.ingredients
            binding.ingredientsContainer.visibility = View.VISIBLE
            hasAdditionalInfo = true
        } else {
            binding.ingredientsContainer.visibility = View.GONE
        }

        // Nutrition facts
        if (!food.nutrition.isNullOrBlank()) {
            binding.tvNutritionFacts.text = food.nutrition
            binding.nutritionContainer.visibility = View.VISIBLE
            hasAdditionalInfo = true
        } else {
            binding.nutritionContainer.visibility = View.GONE
        }

        // Allergens
        if (!food.allergens.isNullOrBlank()) {
            binding.tvAllergens.text = food.allergens
            binding.allergensContainer.visibility = View.VISIBLE
            hasAdditionalInfo = true
        } else {
            binding.allergensContainer.visibility = View.GONE
        }

        // Storage instructions
        if (!food.storageInstructions.isNullOrBlank()) {
            binding.tvStorageInstructions.text = food.storageInstructions
            binding.storageContainer.visibility = View.VISIBLE
            hasAdditionalInfo = true
        } else {
            binding.storageContainer.visibility = View.GONE
        }

        binding.additionalInfoCard.visibility = if (hasAdditionalInfo) View.VISIBLE else View.GONE

        // Add to cart button state
        binding.btnAddToCart.apply {
            isEnabled = food.stock > 0
            text = getString(if (food.stock > 0) R.string.add_to_cart else R.string.out_of_stock)
        }
    }
} 