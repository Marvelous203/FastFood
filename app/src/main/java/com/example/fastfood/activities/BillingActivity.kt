package com.example.fastfood.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fastfood.R
import com.example.fastfood.activities.MainActivity
import com.example.fastfood.adapters.CartAdapter
import com.example.fastfood.databinding.ActivityBillingBinding
import com.example.fastfood.models.PaymentMethod
import com.example.fastfood.utils.ZaloPayQRDialog
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
        setupRealTimeValidation()
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
        val paymentMethods = PaymentMethod.values()
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
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.paymentSuccess.observe(this) { success ->
            if (success == true && !isFinishing && !isDestroyed) {
                showSuccessDialog()
            }
        }

        viewModel.showZaloPayQR.observe(this) { showQR ->
            if (showQR == true && !isFinishing && !isDestroyed) {
                showZaloPayQRDialog()
            }
        }

        viewModel.customerName.observe(this) { name ->
            val nameToSet = name ?: ""
            if (binding.etCustomerName.text.toString() != nameToSet) {
                binding.etCustomerName.setText(nameToSet)
            }
        }

        viewModel.phoneNumber.observe(this) { phone ->
            val phoneToSet = phone ?: ""
            if (binding.etCustomerPhone.text.toString() != phoneToSet) {
                binding.etCustomerPhone.setText(phoneToSet)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnPlaceOrder.setOnClickListener {
            if (validateForm()) {
                // Update ViewModel with current form data
                viewModel.setCustomerName(binding.etCustomerName.text.toString().trim())
                viewModel.setPhoneNumber(binding.etCustomerPhone.text.toString().trim())
                viewModel.setDeliveryAddress(binding.etDeliveryAddress.text.toString().trim())
                viewModel.setDeliveryNote("") // No delivery note field in current layout

                // Process payment
                viewModel.processPayment()
            }
        }
    }

    private fun setupRealTimeValidation() {
        // Customer name validation
        binding.etCustomerName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = s.toString().trim()
                binding.etCustomerName.error = when {
                    name.isEmpty() -> null // Don't show error while typing
                    name.length < 2 -> "Họ tên phải có ít nhất 2 ký tự"
                    !name.matches(Regex("^[a-zA-ZÀ-ỹ\\s]+$")) -> "Họ tên chỉ được chứa chữ cái và khoảng trắng"
                    else -> null
                }
            }
        })

        // Phone number validation
        binding.etCustomerPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val phone = s.toString().trim()
                binding.etCustomerPhone.error = when {
                    phone.isEmpty() -> null // Don't show error while typing
                    phone.length < 10 -> "Số điện thoại phải có ít nhất 10 số"
                    !isValidPhoneNumber(phone) -> "Số điện thoại không hợp lệ"
                    else -> null
                }
            }
        })

        // Delivery address validation
        binding.etDeliveryAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val address = s.toString().trim()
                binding.etDeliveryAddress.error = when {
                    address.isEmpty() -> null // Don't show error while typing
                    address.length < 10 -> "Địa chỉ phải có ít nhất 10 ký tự"
                    address.length > 200 -> "Địa chỉ không được vượt quá 200 ký tự"
                    else -> null
                }
            }
        })
    }

    private fun updateTotals() {
        binding.tvDeliveryFee.text = viewModel.getFormattedDeliveryFee()
        binding.tvTax.text = viewModel.getFormattedTax()
        binding.tvTotal.text = viewModel.getFormattedFinalTotal()
        binding.btnPlaceOrder.text = "Đặt hàng (${viewModel.getFormattedFinalTotal()})"
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Reset previous errors
        binding.etCustomerName.error = null
        binding.etCustomerPhone.error = null
        binding.etDeliveryAddress.error = null

        // Validate customer name
        val customerName = binding.etCustomerName.text.toString().trim()
        if (customerName.isEmpty()) {
            binding.etCustomerName.error = "Vui lòng nhập họ tên"
            isValid = false
        } else if (customerName.length < 2) {
            binding.etCustomerName.error = "Họ tên phải có ít nhất 2 ký tự"
            isValid = false
        } else if (!customerName.matches(Regex("^[a-zA-ZÀ-ỹ\\s]+$"))) {
            binding.etCustomerName.error = "Họ tên chỉ được chứa chữ cái và khoảng trắng"
            isValid = false
        }

        // Validate phone number
        val phoneNumber = binding.etCustomerPhone.text.toString().trim()
        if (phoneNumber.isEmpty()) {
            binding.etCustomerPhone.error = "Vui lòng nhập số điện thoại"
            isValid = false
        } else if (!isValidPhoneNumber(phoneNumber)) {
            binding.etCustomerPhone.error = "Số điện thoại không hợp lệ (10-11 số, bắt đầu bằng 0)"
            isValid = false
        }

        // Validate delivery address
        val deliveryAddress = binding.etDeliveryAddress.text.toString().trim()
        if (deliveryAddress.isEmpty()) {
            binding.etDeliveryAddress.error = "Vui lòng nhập địa chỉ giao hàng"
            isValid = false
        } else if (deliveryAddress.length < 10) {
            binding.etDeliveryAddress.error = "Địa chỉ phải có ít nhất 10 ký tự"
            isValid = false
        } else if (deliveryAddress.length > 200) {
            binding.etDeliveryAddress.error = "Địa chỉ không được vượt quá 200 ký tự"
            isValid = false
        }

        // Validate cart is not empty
        if (viewModel.cartItems.value?.isEmpty() == true) {
            Toast.makeText(this, "Giỏ hàng trống, không thể đặt hàng", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Validate total amount
        if ((viewModel.totalPrice.value ?: 0.0) <= 0) {
            Toast.makeText(this, "Tổng tiền không hợp lệ", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

        private fun isValidPhoneNumber(phone: String): Boolean {
        // Remove any spaces, dashes, dots, or parentheses
        val cleanPhone = phone.replace(Regex("[\\s\\-().+]+"), "")

        // Check if it's a valid Vietnamese phone number
        return when {
            // Mobile numbers starting with 03, 05, 07, 08, 09 (10 digits)
            cleanPhone.matches(Regex("^0[35789]\\d{8}$")) -> true
            // Landline numbers starting with 02 (10 digits)
            cleanPhone.matches(Regex("^02[0-9]\\d{7}$")) -> true
            // International format without country code (start with +84)
            cleanPhone.matches(Regex("^\\+84[35789]\\d{8}$")) -> true
            cleanPhone.matches(Regex("^\\+842[0-9]\\d{7}$")) -> true
            // Alternative format with 84 prefix
            cleanPhone.matches(Regex("^84[35789]\\d{8}$")) -> true
            cleanPhone.matches(Regex("^842[0-9]\\d{7}$")) -> true
            else -> false
        }
    }

    private fun showSuccessDialog() {
        if (isFinishing || isDestroyed) {
            return
        }

        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đặt hàng thành công!")
                .setMessage("Cảm ơn bạn đã đặt hàng. Chúng tôi sẽ liên hệ với bạn sớm nhất.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    // Navigate back to MainActivity and clear back stack
                    if (!isFinishing && !isDestroyed) {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                }
                .setOnDismissListener {
                    // Ensure we finish the activity even if dialog is dismissed
                    if (!isFinishing && !isDestroyed) {
                        finish()
                    }
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            // If dialog can't be shown, just finish the activity
            if (!isFinishing) {
                finish()
            }
        }
    }

    private fun showZaloPayQRDialog() {
        val totalAmount = viewModel.totalPrice.value ?: 0.0

        val qrDialog = ZaloPayQRDialog(
            context = this,
            amount = totalAmount,
            onPaymentSuccess = {
                viewModel.completeZaloPayPayment()
            },
            onPaymentCancel = {
                viewModel.cancelZaloPayPayment()
            }
        )

        qrDialog.show()
    }
}
