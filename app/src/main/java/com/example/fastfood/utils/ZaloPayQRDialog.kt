package com.example.fastfood.utils

import android.app.Dialog
import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.fastfood.databinding.DialogZalopayQrBinding
import java.text.NumberFormat
import java.util.*

class ZaloPayQRDialog(
    private val context: Context,
    private val amount: Double,
    private val orderCode: String = "ZP${System.currentTimeMillis()}",
    private val onPaymentSuccess: () -> Unit,
    private val onPaymentCancel: () -> Unit
) {

    private var dialog: Dialog? = null
    private var binding: DialogZalopayQrBinding? = null
    private var countDownTimer: CountDownTimer? = null

    fun show() {
        binding = DialogZalopayQrBinding.inflate(LayoutInflater.from(context))

        dialog = Dialog(context).apply {
            setContentView(binding!!.root)

            // Set dialog properties
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setCancelable(false)
        }

        setupViews()
        startCountdown()

        dialog?.show()
    }

    private fun setupViews() {
        binding?.apply {
            // Set amount
            tvAmount.text = formatCurrency(amount)

            // Set order code
            tvOrderCode.text = orderCode

            // Set click listeners
            btnClose.setOnClickListener {
                dismiss()
                onPaymentCancel()
            }

            btnCancel.setOnClickListener {
                dismiss()
                onPaymentCancel()
            }

            btnPaymentSuccess.setOnClickListener {
                dismiss()
                onPaymentSuccess()
            }
        }
    }

    private fun startCountdown() {
        // 15 minutes countdown
        countDownTimer = object : CountDownTimer(15 * 60 * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding?.tvExpireTime?.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                binding?.tvExpireTime?.text = "00:00"
                dismiss()
                onPaymentCancel()
            }
        }.start()
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getNumberInstance(Locale("vi", "VN"))
            .format(amount) + "₫"
    }

    fun dismiss() {
        countDownTimer?.cancel()
        dialog?.dismiss()
        dialog = null
        binding = null
    }
}
