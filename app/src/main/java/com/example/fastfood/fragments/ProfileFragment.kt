package com.example.fastfood.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.fastfood.R
import com.example.fastfood.activities.AuthActivity
import com.example.fastfood.databinding.FragmentProfileBinding
import com.example.fastfood.utils.PreferencesManager

class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        setupViews()
        setupClickListeners()
    }
    
    private fun setupViews() {
        if (preferencesManager.isLoggedIn) {
            showUserProfile()
        } else {
            showGuestProfile()
        }
    }
    
    private fun showUserProfile() {
        val user = preferencesManager.currentUser
        
        binding.layoutUserInfo.visibility = View.VISIBLE
        binding.layoutGuestInfo.visibility = View.GONE
        
        binding.tvUserName.text = user?.fullName ?: "User"
        binding.tvUserEmail.text = user?.email ?: ""
        binding.tvUserPhone.text = user?.phone ?: "Chưa cập nhật"
        
        // Show logout option
        binding.itemLogout.visibility = View.VISIBLE
        binding.itemLogin.visibility = View.GONE
    }
    
    private fun showGuestProfile() {
        binding.layoutUserInfo.visibility = View.GONE
        binding.layoutGuestInfo.visibility = View.VISIBLE
        
        // Hide logout option
        binding.itemLogout.visibility = View.GONE
        binding.itemLogin.visibility = View.VISIBLE
    }
    
    private fun setupClickListeners() {
        // Edit Profile
        binding.itemEditProfile.setOnClickListener {
            if (preferencesManager.isLoggedIn) {
                // TODO: Navigate to edit profile
                Toast.makeText(context, "Tính năng chỉnh sửa hồ sơ sẽ có sớm", Toast.LENGTH_SHORT).show()
            } else {
                showLoginPrompt()
            }
        }
        
        // Delivery Addresses
        binding.itemDeliveryAddresses.setOnClickListener {
            if (preferencesManager.isLoggedIn) {
                // TODO: Navigate to addresses
                Toast.makeText(context, "Tính năng địa chỉ giao hàng sẽ có sớm", Toast.LENGTH_SHORT).show()
            } else {
                showLoginPrompt()
            }
        }
        
        // Payment Methods
        binding.itemPaymentMethods.setOnClickListener {
            if (preferencesManager.isLoggedIn) {
                // TODO: Navigate to payment methods
                Toast.makeText(context, "Tính năng phương thức thanh toán sẽ có sớm", Toast.LENGTH_SHORT).show()
            } else {
                showLoginPrompt()
            }
        }
        
        // Notifications
        binding.itemNotifications.setOnClickListener {
            // TODO: Navigate to notification settings
            Toast.makeText(context, "Tính năng thông báo sẽ có sớm", Toast.LENGTH_SHORT).show()
        }
        
        // Store Location
        binding.itemStoreLocation.setOnClickListener {
            val intent = Intent(requireContext(), com.example.fastfood.activities.MapActivity::class.java)
            startActivity(intent)
        }
        
        // Help & Support
        binding.itemHelpSupport.setOnClickListener {
            // TODO: Navigate to help
            Toast.makeText(context, "Tính năng hỗ trợ sẽ có sớm", Toast.LENGTH_SHORT).show()
        }
        
        // About
        binding.itemAbout.setOnClickListener {
            // TODO: Show about dialog
            showAboutDialog()
        }
        
        // Login (for guest)
        binding.itemLogin.setOnClickListener {
            navigateToAuth()
        }
        
        binding.btnLogin.setOnClickListener {
            navigateToAuth()
        }
        
        // Logout
        binding.itemLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }
    
    private fun showLoginPrompt() {
        Toast.makeText(context, "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_SHORT).show()
    }
    
    private fun navigateToAuth() {
        val intent = Intent(requireContext(), AuthActivity::class.java)
        startActivity(intent)
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Đăng xuất")
            .setMessage(getString(R.string.confirm_logout))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                performLogout()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
    
    private fun performLogout() {
        preferencesManager.logout()
        Toast.makeText(context, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show()
        setupViews() // Refresh the UI
    }
    
    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.about))
            .setMessage("""
                FastFood App
                
                Ứng dụng đặt đồ ăn nhanh tiện lợi
                Phiên bản: 1.0
                
                Được phát triển bởi nhóm FastFood Team
                
                © 2024 FastFood. All rights reserved.
            """.trimIndent())
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh views when returning to this fragment
        setupViews()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 