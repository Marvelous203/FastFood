package com.example.fastfood.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fastfood.R
import com.example.fastfood.adapters.CartAdapter
import com.example.fastfood.databinding.ActivityBillingBinding
import com.example.fastfood.viewmodels.BillingViewModel

class BillingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBillingBinding
    private val viewModel: BillingViewModel by viewModels()
    private lateinit var cartAdapter: CartAdapter
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, BillingActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupPaymentMethods()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Thanh toán"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { foodId, quantity ->
                // Not allowing quantity changes in billing screen
                Toast.makeText(this, "Quay lại giỏ hàng để thay đổi số lượng", Toast.LENGTH_SHORT).show()
                false // Return false to indicate failure
            },
            onRemoveClick = { foodId ->
                // Not allowing removal in billing screen
                Toast.makeText(this, "Quay lại giỏ hàng để xóa sản phẩm", Toast.LENGTH_SHORT).show()
            },
            getProductDetails = { productId ->
                viewModel.getProductDetails(productId)
            }
        )
        
        binding.rvOrderItems.apply {
            layoutManager = LinearLayoutManager(this@BillingActivity)
            adapter = cartAdapter
            isNestedScrollingEnabled = false // Since it's inside ScrollView
        }
    }
    
    private fun setupPaymentMethods() {
        val paymentMethods = BillingViewModel.PaymentMethod.values()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            paymentMethods.map { it.displayName }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPaymentMethod.adapter = adapter
        
        binding.spinnerPaymentMethod.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                viewModel.setPaymentMethod(paymentMethods[position])
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }
    
    private fun setupObservers() {
        viewModel.cartItems.observe(this) { items ->
            cartAdapter.submitList(items.toList())
            
            if (items.isEmpty()) {
                Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        
        viewModel.totalPrice.observe(this) { subtotal ->
            binding.tvSubtotal.text = "${String.format("%,.0f", subtotal)} ₫"
            updateTotals()
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnPlaceOrder.isEnabled = !isLoading
            binding.btnPlaceOrder.text = if (isLoading) "Đang xử lý..." else "Đặt hàng"
        }
        
        viewModel.error.observe(this) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
        
        viewModel.paymentSuccess.observe(this) { success ->
            if (success) {
                showSuccessDialog()
            }
        }
        
        viewModel.customerName.observe(this) { name ->
            if (binding.etCustomerName.text.toString() != name) {
                binding.etCustomerName.setText(name)
            }
        }
        
        viewModel.phoneNumber.observe(this) { phone ->
            if (binding.etCustomerPhone.text.toString() != phone) {
                binding.etCustomerPhone.setText(phone)
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnPlaceOrder.setOnClickListener {
            // Update ViewModel with current form data
            viewModel.setCustomerName(binding.etCustomerName.text.toString().trim())
            viewModel.setPhoneNumber(binding.etCustomerPhone.text.toString().trim())
            viewModel.setDeliveryAddress(binding.etDeliveryAddress.text.toString().trim())
            viewModel.setDeliveryNote("") // No delivery note field in current layout
            
            // Process payment
            viewModel.processPayment()
        }
    }
    
    private fun updateTotals() {
        binding.tvDeliveryFee.text = viewModel.getFormattedDeliveryFee()
        binding.tvTax.text = viewModel.getFormattedTax()
        binding.tvTotal.text = viewModel.getFormattedFinalTotal()
        binding.btnPlaceOrder.text = "Đặt hàng (${viewModel.getFormattedFinalTotal()})"
    }
    
    private fun showSuccessDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Đặt hàng thành công!")
            .setMessage("Cảm ơn bạn đã đặt hàng. Chúng tôi sẽ liên hệ với bạn sớm nhất.")
            .setPositiveButton("OK") { _, _ ->
                // Navigate back to MainActivity and clear back stack
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }
} 