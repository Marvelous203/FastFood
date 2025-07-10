package com.example.fastfood.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fastfood.R
import com.example.fastfood.activities.AuthActivity
import com.example.fastfood.databinding.FragmentRegisterBinding
import com.example.fastfood.models.RegisterRequest
import com.example.fastfood.models.AuthResponse
import com.example.fastfood.models.User
import com.example.fastfood.network.RetrofitClient
import com.example.fastfood.network.ApiService
import com.example.fastfood.utils.PreferencesManager
import com.example.fastfood.utils.SimpleApiTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class RegisterFragment : Fragment() {
    
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var apiService: ApiService
    
    companion object {
        private const val TAG = "RegisterFragment"
        private const val NETWORK_TIMEOUT = 10000L // 10 seconds
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        apiService = RetrofitClient.create(ApiService::class.java)
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            if (validateInput()) {
                performRegister()
            }
        }
        
        binding.tvLogin.setOnClickListener {
            (activity as? AuthActivity)?.switchToLogin()
        }
    }
    
    private fun validateInput(): Boolean {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim().takeIf { it.isNotEmpty() }
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
        // Reset errors
        binding.tilFirstName.error = null
        binding.tilLastName.error = null
        binding.tilEmail.error = null
        binding.tilPhone.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        
        var isValid = true
        
        // Validate first name
        if (firstName.isEmpty()) {
            binding.tilFirstName.error = getString(R.string.field_required)
            isValid = false
        }
        
        // Validate last name (required by API)
        if (lastName.isEmpty()) {
            binding.tilLastName.error = getString(R.string.field_required)
            isValid = false
        }
        
        // Validate email
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.field_required)
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.invalid_email)
            isValid = false
        }
        
        // Phone is optional but validate if provided
        if (phone != null && phone.isNotEmpty() && phone.length < 10) {
            binding.tilPhone.error = getString(R.string.invalid_phone)
            isValid = false
        }
        
        // Validate password
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.field_required)
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.password_too_short)
            isValid = false
        }
        
        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.field_required)
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.passwords_not_match)
            isValid = false
        }
        
        return isValid
    }
    
    private fun performRegister() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim().takeIf { it.isNotEmpty() }
        val password = binding.etPassword.text.toString()
        
        Log.d(TAG, "📝 Starting registration process for: $email")
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                // Test API connectivity first
                Log.d(TAG, "🔍 Testing API connectivity...")
                if (!testApiConnectivity()) {
                    Log.e(TAG, "❌ API connectivity test failed")
                    showError(getString(R.string.error_network))
                    return@launch
                }
                
                Log.d(TAG, "✅ API connectivity OK, proceeding with registration...")
                
                withContext(Dispatchers.IO) {
                    val request = RegisterRequest(
                        email = email,
                        password = password,
                        firstName = firstName,
                        lastName = lastName,
                        phone = phone
                    )
                    
                    // Add timeout wrapper
                    val response = withTimeoutOrNull(NETWORK_TIMEOUT) {
                        try {
                            Log.d(TAG, "📡 Sending registration request...")
                            apiService.register(request)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Registration request error: ${e.message}", e)
                            throw e
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (response == null) {
                            Log.e(TAG, "⏰ Registration request timed out")
                            showError(getString(R.string.error_network))
                            return@withContext
                        }
                        
                        Log.d(TAG, "📨 Received response: ${response.code()}")
                        
                        if (response.isSuccessful) {
                            val responseCode = response.code()
                            val authResponse = response.body()
                            Log.d(TAG, "✅ Response successful, processing...")
                            Log.d(TAG, "📦 Response body: $authResponse")
                            Log.d(TAG, "🔍 Success field: ${authResponse?.success}")
                            Log.d(TAG, "🔑 Token field: ${authResponse?.token}")
                            Log.d(TAG, "👤 User field: ${authResponse?.user}")
                            
                            when (responseCode) {
                                200 -> {
                                    // Standard success with token and user
                                    if (authResponse?.token != null && authResponse.user != null) {
                                        // Save user data and token
                                        authResponse.token.let { token ->
                                            preferencesManager.authToken = token
                                            Log.d(TAG, "🔑 Token saved")
                                        }
                                        authResponse.user.let { user ->
                                            preferencesManager.currentUser = user
                                            Log.d(TAG, "👤 User data saved: ${user.email}")
                                        }
                                        preferencesManager.isLoggedIn = true
                                        
                                        Log.d(TAG, "🎉 Registration with auto-login successful!")
                                        Toast.makeText(context, getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                                        
                                        // Navigate back to main app
                                        (activity as? AuthActivity)?.navigateToMain()
                                    } else {
                                        val errorMsg = authResponse?.message ?: getString(R.string.register_failed)
                                        Log.e(TAG, "❌ Registration failed: $errorMsg")
                                        showError(errorMsg)
                                    }
                                }
                                201, 204 -> {
                                    // User created successfully but needs email confirmation
                                    Log.d(TAG, "✅ Registration successful - email confirmation may be required")
                                    Toast.makeText(context, "Đăng ký thành công! Vui lòng kiểm tra email để xác nhận tài khoản, sau đó đăng nhập.", Toast.LENGTH_LONG).show()
                                    
                                    // Switch back to login
                                    (activity as? AuthActivity)?.switchToLogin()
                                }
                                else -> {
                                    val errorMsg = authResponse?.message ?: getString(R.string.register_failed)
                                    Log.e(TAG, "❌ Registration failed: $errorMsg")
                                    showError(errorMsg)
                                }
                            }
                        } else {
                            val errorMsg = when (response.code()) {
                                409 -> getString(R.string.error_email_exists)
                                422 -> getString(R.string.error_required_fields)
                                400 -> getString(R.string.error_invalid_credentials)
                                500 -> getString(R.string.error_server)
                                else -> getString(R.string.register_failed)
                            }
                            Log.e(TAG, "❌ HTTP Error ${response.code()}: $errorMsg")
                            showError(errorMsg)
                        }
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "⏰ Socket timeout during registration", e)
                showError(getString(R.string.error_network))
            } catch (e: UnknownHostException) {
                Log.e(TAG, "🌐 DNS resolution failed during registration", e)
                showError(getString(R.string.error_network))
            } catch (e: SSLException) {
                Log.e(TAG, "🔒 SSL error during registration", e)
                showError(getString(R.string.error_network))
            } catch (e: Exception) {
                Log.e(TAG, "❌ Unexpected error during registration", e)
                showError(getString(R.string.error_server))
            } finally {
                showLoading(false)
            }
        }
    }
    
    private suspend fun testApiConnectivity(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔍 Testing basic connectivity...")
                val canReachInternet = SimpleApiTester.basicInternetTest()
                if (!canReachInternet) {
                    Log.e(TAG, "❌ No internet connection")
                    return@withContext false
                }
                
                Log.d(TAG, "🏓 Testing API server...")
                val canReachApi = SimpleApiTester.pingTest()
                if (!canReachApi) {
                    Log.e(TAG, "❌ Cannot reach API server")
                    return@withContext false
                }
                
                Log.d(TAG, "✅ All connectivity tests passed")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Connectivity test failed", e)
                return@withContext false
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
        
        if (show) {
            Log.d(TAG, "🔄 Showing loading state")
        } else {
            Log.d(TAG, "✋ Hiding loading state")
        }
    }
    
    private fun showError(message: String) {
        Log.e(TAG, "⚠️ Showing error: $message")
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 