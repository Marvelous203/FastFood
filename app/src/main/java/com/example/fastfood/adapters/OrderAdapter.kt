package com.example.fastfood.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fastfood.R
import com.example.fastfood.databinding.ItemOrderBinding
import com.example.fastfood.models.Order
import com.example.fastfood.models.PaymentMethod
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(
    private val onOrderClick: (Order) -> Unit,
    private val onCancelOrder: (Order) -> Unit,
    private val onReorder: (Order) -> Unit
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding, parent.context)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        android.util.Log.d("OrderAdapter", "Binding order at position $position: ${order.id}")
        holder.bind(order)
    }

    override fun submitList(list: List<Order>?) {
        android.util.Log.d("OrderAdapter", "submitList called with ${list?.size ?: 0} orders")
        super.submitList(list)
    }

    inner class OrderViewHolder(
        private val binding: ItemOrderBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
        private val numberFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        fun bind(order: Order) {
            binding.apply {
                // Order basic info
                tvOrderId.text = "#${(order.id ?: "UNKNOWN").take(8).uppercase()}"
                tvOrderDate.text = order.createdAt ?: "N/A"
                tvTotalAmount.text = formatCurrency(order.totalAmount ?: 0.0)

                // Order status
                setupOrderStatus(order.status)

                // Order items
                setupOrderItems(order)

                // Payment method
                setupPaymentMethod(order.paymentMethod)

                // Action buttons
                setupActionButtons(order)

                // Click listeners
                root.setOnClickListener { onOrderClick(order) }
                btnViewDetails.setOnClickListener { onOrderClick(order) }
                btnCancelOrder.setOnClickListener { onCancelOrder(order) }
                btnReorder.setOnClickListener { onReorder(order) }
            }
        }

        private fun setupOrderStatus(status: String) {
            val (statusText, statusColor) = when (status.uppercase()) {
                "PENDING" -> "Chờ xác nhận" to R.color.orange_500
                "CONFIRMED" -> "Đã xác nhận" to R.color.blue_500
                "PREPARING" -> "Đang chuẩn bị" to R.color.yellow_600
                "READY" -> "Sẵn sàng" to R.color.green_500
                "DELIVERING" -> "Đang giao" to R.color.blue_600
                "DELIVERED" -> "Đã giao" to R.color.green_600
                "CANCELLED" -> "Đã hủy" to R.color.red_500
                "COMPLETED" -> "Hoàn thành" to R.color.green_700
                else -> status to R.color.text_secondary
            }

            binding.tvOrderStatus.apply {
                text = statusText
                backgroundTintList = ContextCompat.getColorStateList(context, statusColor)
            }
        }

        private fun setupOrderItems(order: Order) {
            val items = order.items ?: emptyList()
            val totalItems = items.sumOf { it.quantity }
            val totalProducts = items.size

            // Create items summary
            val itemsSummary = items.take(3).joinToString(", ") { item ->
                "${item.foodName} x${item.quantity}"
            }.let { summary ->
                if (items.size > 3) "$summary..." else summary
            }

            binding.tvOrderItems.text = itemsSummary
            binding.tvItemsCount.text = "$totalItems món • $totalProducts sản phẩm"
        }

        private fun setupPaymentMethod(paymentMethod: String) {
            val methodText = when (paymentMethod.uppercase()) {
                "CASH" -> "Thanh toán tiền mặt"
                "ZALOPAY" -> "ZaloPay"
                "CREDIT_CARD" -> "Thẻ tín dụng"
                "BANK_TRANSFER" -> "Chuyển khoản ngân hàng"
                else -> "Không xác định"
            }

            binding.tvPaymentMethod.text = methodText
        }

        private fun setupActionButtons(order: Order) {
            val status = order.status.uppercase()

            // Cancel button - only show for pending orders
            binding.btnCancelOrder.visibility = if (status == "PENDING") {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Reorder button - always available except for cancelled orders
            binding.btnReorder.visibility = if (status == "CANCELLED") {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        private fun formatCurrency(amount: Double): String {
            return NumberFormat.getNumberInstance(Locale("vi", "VN"))
                .format(amount) + "₫"
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}
