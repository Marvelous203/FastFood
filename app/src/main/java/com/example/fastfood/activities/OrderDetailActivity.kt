package com.example.fastfood.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fastfood.R
import com.example.fastfood.adapters.OrderItemAdapter
import com.example.fastfood.databinding.ActivityOrderDetailBinding
import com.example.fastfood.models.Order
import com.example.fastfood.viewmodels.OrderDetailViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private val viewModel: OrderDetailViewModel by viewModels()
    private lateinit var orderItemAdapter: OrderItemAdapter

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
    private val numberFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val orderId = intent.getStringExtra("order_id")
        if (orderId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        setupRecyclerView()
        setupObservers()

        viewModel.loadOrderDetail(orderId)
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.btnCallSupport.setOnClickListener {
            val phoneNumber = "1900123456" // Replace with actual support number
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        orderItemAdapter = OrderItemAdapter()
        binding.rvOrderItems.apply {
            layoutManager = LinearLayoutManager(this@OrderDetailActivity)
            adapter = orderItemAdapter
        }
    }

    private fun setupObservers() {
        viewModel.order.observe(this) { order ->
            order?.let { displayOrderDetails(it) }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.scrollView.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }

        viewModel.successMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }
    }

    private fun displayOrderDetails(order: Order) {
        binding.apply {
            // Order header
            tvOrderId.text = "#${(order.id ?: "UNKNOWN").take(8).uppercase()}"
            tvOrderDate.text = order.createdAt ?: "N/A"

            // Order status
            setupOrderStatus(order.status)

            // Order items
            orderItemAdapter.submitList(order.items ?: emptyList())

            // Delivery information
            tvDeliveryAddress.text = order.deliveryAddress ?: "Chưa có địa chỉ"
            tvDeliveryPhone.text = order.phone
            tvOrderNote.text = order.note ?: "Không có ghi chú"

            // Payment information
            setupPaymentInfo(order)

            // Order total
            setupOrderTotal(order)

            // Action buttons
            setupActionButtons(order)
        }
    }

    private fun setupOrderStatus(status: String) {
        val (statusText, statusColor, description) = when (status.uppercase()) {
            "PENDING" -> Triple("Chờ xác nhận", R.color.orange_500, "Đơn hàng đang chờ xác nhận từ cửa hàng")
            "CONFIRMED" -> Triple("Đã xác nhận", R.color.blue_500, "Đơn hàng đã được xác nhận và đang chuẩn bị")
            "PREPARING" -> Triple("Đang chuẩn bị", R.color.yellow_600, "Cửa hàng đang chuẩn bị đơn hàng của bạn")
            "READY" -> Triple("Sẵn sàng", R.color.green_500, "Đơn hàng đã sẵn sàng để giao")
            "DELIVERING" -> Triple("Đang giao", R.color.blue_600, "Đơn hàng đang được giao đến bạn")
            "DELIVERED" -> Triple("Đã giao", R.color.green_600, "Đơn hàng đã được giao thành công")
            "CANCELLED" -> Triple("Đã hủy", R.color.red_500, "Đơn hàng đã bị hủy")
            "COMPLETED" -> Triple("Hoàn thành", R.color.green_700, "Đơn hàng đã hoàn thành")
            else -> Triple(status, R.color.text_secondary, "")
        }

        binding.tvOrderStatus.apply {
            text = statusText
            backgroundTintList = ContextCompat.getColorStateList(this@OrderDetailActivity, statusColor)
        }

        binding.tvStatusDescription.text = description
    }

    private fun setupPaymentInfo(order: Order) {
        val paymentMethodText = when (order.paymentMethod.uppercase()) {
            "CASH" -> "Thanh toán tiền mặt"
            "ZALOPAY" -> "ZaloPay"
            "CREDIT_CARD" -> "Thẻ tín dụng"
            "BANK_TRANSFER" -> "Chuyển khoản ngân hàng"
            else -> "Không xác định"
        }

        binding.tvPaymentMethod.text = paymentMethodText

        // You can add payment status here if available
        binding.tvPaymentStatus.text = "Chưa thanh toán" // Or get from payment info
    }

    private fun setupOrderTotal(order: Order) {
        val subtotal = order.items?.sumOf { it.quantity * it.price } ?: 0.0
        val deliveryFee = 15000.0 // You can make this dynamic
        val discount = 0.0 // Get from order if available
        val total = order.totalAmount ?: 0.0

        binding.apply {
            tvSubtotal.text = "${numberFormat.format(subtotal)}₫"
            tvDeliveryFee.text = "${numberFormat.format(deliveryFee)}₫"
            tvDiscount.text = if (discount > 0) "-${numberFormat.format(discount)}₫" else "0₫"
            tvTotal.text = "${numberFormat.format(total)}₫"
        }
    }

    private fun setupActionButtons(order: Order) {
        val status = order.status.uppercase()

        // Cancel button
        binding.btnCancelOrder.visibility = if (status == "PENDING") View.VISIBLE else View.GONE
        binding.btnCancelOrder.setOnClickListener {
            showCancelOrderDialog(order)
        }

        // Reorder button
        binding.btnReorder.setOnClickListener {
            viewModel.reorder(order)
        }

        // Track order button (show for active orders)
        binding.btnTrackOrder.visibility = if (status in listOf("CONFIRMED", "PREPARING", "READY", "DELIVERING")) {
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.btnTrackOrder.setOnClickListener {
            // Open map or tracking screen
            Toast.makeText(this, "Tính năng theo dõi đơn hàng sẽ sớm có", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCancelOrderDialog(order: Order) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xác nhận hủy đơn hàng")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này? Hành động này không thể hoàn tác.")
            .setPositiveButton("Hủy đơn") { _, _ ->
                viewModel.cancelOrder(order)
            }
            .setNegativeButton("Không", null)
            .show()
    }
}
