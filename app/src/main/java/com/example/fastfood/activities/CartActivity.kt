package com.example.fastfood.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fastfood.R
import com.example.fastfood.adapters.CartAdapter
import com.example.fastfood.databinding.ActivityCartBinding
import com.example.fastfood.viewmodels.CartViewModel

class CartActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCartBinding
    private val viewModel: CartViewModel by viewModels()
    private lateinit var cartAdapter: CartAdapter
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, CartActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Giỏ hàng"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { foodId, quantity ->
                viewModel.updateQuantity(foodId, quantity)
                true // Return true to indicate success
            },
            onRemoveClick = { foodId ->
                viewModel.removeFromCart(foodId)
            },
            getProductDetails = { productId ->
                viewModel.getProductDetails(productId)
            }
        )
        
        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartAdapter
        }
    }
    
    private fun setupObservers() {
        viewModel.cartItems.observe(this) { items ->
            cartAdapter.submitList(items.toList())
            updateEmptyState(items.isEmpty())
        }
        
        viewModel.totalPrice.observe(this) { total ->
            binding.tvTotalPrice.text = "${String.format("%,.0f", total)} ₫"
            binding.btnCheckout.text = "Thanh toán (${String.format("%,.0f", total)} ₫)"
        }
        
        viewModel.itemCount.observe(this) { count ->
            supportActionBar?.title = "Giỏ hàng ($count)"
        }
        
        viewModel.message.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.navigateToCheckout.observe(this) { navigate ->
            if (navigate) {
                BillingActivity.start(this)
                viewModel.onCheckoutNavigated()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnCheckout.setOnClickListener {
            viewModel.proceedToCheckout()
        }
        
        binding.btnClearCart.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa giỏ hàng")
                .setMessage("Bạn có chắc chắn muốn xóa tất cả sản phẩm?")
                .setPositiveButton("Xóa") { _, _ ->
                    viewModel.clearCart()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.rvCartItems.visibility = android.view.View.GONE
            binding.layoutCartSummary.visibility = android.view.View.GONE
            binding.layoutEmptyCart.visibility = android.view.View.VISIBLE
        } else {
            binding.rvCartItems.visibility = android.view.View.VISIBLE
            binding.layoutCartSummary.visibility = android.view.View.VISIBLE
            binding.layoutEmptyCart.visibility = android.view.View.GONE
        }
    }
} 