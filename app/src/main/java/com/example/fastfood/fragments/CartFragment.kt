package com.example.fastfood.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fastfood.R
import com.example.fastfood.activities.BillingActivity
import com.example.fastfood.adapters.CartAdapter
import com.example.fastfood.models.CartItem
import com.example.fastfood.network.RetrofitClient
import com.example.fastfood.utils.CartManager
import com.example.fastfood.utils.PreferencesManager
import com.example.fastfood.utils.Result
import kotlinx.coroutines.launch

class CartFragment : Fragment() {
    private lateinit var cartManager: CartManager
    private lateinit var cartAdapter: CartAdapter
    private lateinit var preferencesManager: PreferencesManager
    private val apiService = RetrofitClient.getApiService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cartManager = CartManager(requireContext())
        preferencesManager = PreferencesManager(requireContext())

        // Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.cart_recycler_view)
        cartAdapter = CartAdapter(
            onQuantityChanged = { foodId: String, newQuantity: Int ->
                // Handle quantity change through API
                try {
                    val result = cartManager.updateQuantity(foodId, newQuantity)
                    when (result) {
                        is Result.Success -> {
                            // Reload cart items after successful update
                            loadCartItems()
                            true
                        }
                        is Result.Failure -> {
                            Toast.makeText(context, result.exception.message, Toast.LENGTH_SHORT).show()
                            false
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi khi cập nhật số lượng: ${e.message}", Toast.LENGTH_SHORT).show()
                    false
                }
            },
            onRemoveClick = { foodId: String ->
                // Handle item removal through API
                lifecycleScope.launch {
                    try {
                        val result = cartManager.removeFromCart(foodId)
                        when (result) {
                            is Result.Success<Unit> -> {
                                // Reload cart items after successful removal
                                loadCartItems()
                            }
                            is Result.Failure -> {
                                Toast.makeText(context, result.exception.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Lỗi khi xóa sản phẩm: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            getProductDetails = { productId ->
                try {
                    val response = apiService.getProductById(productId)
                    if (response.isSuccessful && response.body() != null) {
                        response.body()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        )
                recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = cartAdapter
        }

        // Setup checkout button
        val checkoutButton = view.findViewById<Button>(R.id.checkout_button)
        checkoutButton.setOnClickListener {
            handleCheckout()
        }

        // Load cart items
        loadCartItems()
    }

    private fun loadCartItems() {
        if (!preferencesManager.isLoggedIn) {
            Toast.makeText(context, "Vui lòng đăng nhập để xem giỏ hàng", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val result = cartManager.fetchCart()
                when (result) {
                    is Result.Success<List<CartItem>> -> {
                        cartAdapter.submitList(result.data)
                        updateTotalPrice()
                    }
                    is Result.Failure -> {
                        Toast.makeText(context, result.exception.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi tải giỏ hàng: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

        private fun updateTotalPrice() {
        lifecycleScope.launch {
            try {
                val totalPrice = cartManager.getTotalPrice()
                // Update total price UI
                view?.findViewById<android.widget.TextView>(R.id.total_price)?.text =
                    getString(R.string.price_format, totalPrice)
            } catch (e: Exception) {
                // Handle error
                Toast.makeText(requireContext(), "Không thể tính tổng tiền: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleCheckout() {
        // Check if user is logged in
        if (!preferencesManager.isLoggedIn) {
            Toast.makeText(context, "Vui lòng đăng nhập để thanh toán", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if cart is empty
        val currentCartItems = cartAdapter.currentList
        if (currentCartItems.isEmpty()) {
            Toast.makeText(context, "Giỏ hàng trống", Toast.LENGTH_SHORT).show()
            return
        }

        // Calculate total amount
        lifecycleScope.launch {
            try {
                val totalAmount = cartManager.getTotalPrice()
                if (totalAmount <= 0) {
                    Toast.makeText(context, "Tổng tiền không hợp lệ", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Navigate to billing activity
                try {
                    val intent = Intent(requireContext(), BillingActivity::class.java)
                    intent.putExtra("total_amount", totalAmount)
                    intent.putExtra("cart_items_count", currentCartItems.size)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Không thể mở trang thanh toán: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Không thể tính tổng tiền: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadCartItems()
    }
}
