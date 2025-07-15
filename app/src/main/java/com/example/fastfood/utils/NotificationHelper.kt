package com.example.fastfood.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.fastfood.R
import com.example.fastfood.activities.CartActivity

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "fastfood_cart_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val cartManager = CartManager(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Cart Notifications"
            val descriptionText = "Notifications about items in your cart"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showCartNotification(itemCount: Int, totalPrice: Double) {
        val intent = Intent(context, CartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedPrice = "${String.format("%,.0f", totalPrice)} ₫"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shopping_cart)
            .setContentTitle("Giỏ hàng của bạn")
            .setContentText("$itemCount sản phẩm - Tổng: $formattedPrice")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Bạn có $itemCount sản phẩm trong giỏ hàng với tổng giá trị $formattedPrice. Nhấn để xem chi tiết và thanh toán."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_shopping_cart,
                "Xem giỏ hàng",
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "Notification permission not granted", e)
        }
    }

    fun hideCartNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    suspend fun checkAndShowCartNotification() {
        val itemCount = cartManager.getItemCount()
        val totalPrice = cartManager.getTotalPrice()

        if (itemCount > 0) {
            showCartNotification(itemCount, totalPrice)
        } else {
            hideCartNotification()
        }
    }
}
