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
import com.example.fastfood.databinding.FragmentLoginBinding
import com.example.fastfood.models.LoginRequest
import com.example.fastfood.network.ApiService
import com.example.fastfood.network.RetrofitClient
import com.example.fastfood.utils.PreferencesManager
import com.example.fastfood.utils.SimpleApiTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var apiService: ApiService
    
    companion object {
        private const val TAG = "LoginFragment"
        private const val NETWORK_TIMEOUT = 10000L // 10 seconds
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        apiService = RetrofitClient.create(ApiService::class.java)
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            if (validateInput()) {
                performLogin()
            }
        }
        
        binding.btnGuest.setOnClickListener {
            (activity as? AuthActivity)?.navigateToMain()
        }
        
        binding.tvForgotPassword.setOnClickListener {
            // TODO: Implement forgot password
            Toast.makeText(context, "Forgot password feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun validateInput(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.field_required)
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.invalid_email)
            return false
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.field_required)
            return false
        }
        
        if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.password_too_short)
            return false
        }
        
        // Clear errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        
        return true
    }
    
    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        Log.d(TAG, "🔐 Starting login process for: $email")
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                // Test API connectivity first
                Log.d(TAG, "🔍 Testing API connectivity...")
                if (!testApiConnectivity()) {
                    Log.e(TAG, "❌ API connectivity test failed")
                    showError("Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.")
                    return@launch
                }
                
                Log.d(TAG, "✅ API connectivity OK, proceeding with login...")
                
                withContext(Dispatchers.IO) {
                    val loginRequest = LoginRequest(email, password)
                    
                    // Add timeout wrapper
                    val response = withTimeoutOrNull(NETWORK_TIMEOUT) {
                        try {
                            Log.d(TAG, "📡 Sending login request...")
                            apiService.login(loginRequest)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Login request error: ${e.message}", e)
                            throw e
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (response == null) {
                            Log.e(TAG, "⏰ Login request timed out")
                            showError("Yêu cầu đăng nhập bị timeout. Vui lòng thử lại.")
                            return@withContext
                        }
                        
                        Log.d(TAG, "📨 Received response: ${response.code()}")
                        
                        if (response.isSuccessful) {
                            val authResponse = response.body()
                            Log.d(TAG, "✅ Response successful, processing...")
                            Log.d(TAG, "📦 Response body: $authResponse")
                            Log.d(TAG, "🔍 Success field: ${authResponse?.success}")
                            Log.d(TAG, "🔑 Token field: ${authResponse?.token}")
                            Log.d(TAG, "👤 User field: ${authResponse?.user}")
                            
                            // Check if we have token and user (API might not return success field)
                            if (authResponse?.token != null && authResponse.user != null) {
                                // Save auth data
                                authResponse.token.let { 
                                    preferencesManager.authToken = it 
                                    Log.d(TAG, "🔑 Token saved")
                                }
                                authResponse.user.let { 
                                    preferencesManager.currentUser = it 
                                    Log.d(TAG, "👤 User data saved: ${it.email}")
                                }
                                preferencesManager.isLoggedIn = true
                                
                                Log.d(TAG, "🎉 Login successful!")
                                Toast.makeText(context, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                                (activity as? AuthActivity)?.navigateToMain()
                            } else {
                                val errorMsg = authResponse?.message ?: "Đăng nhập thất bại - không nhận được token hoặc thông tin user"
                                Log.e(TAG, "❌ Login failed: $errorMsg")
                                showError(errorMsg)
                            }
                        } else {
                            val errorMsg = when (response.code()) {
                                401 -> "Email hoặc mật khẩu không đúng"
                                403 -> "Tài khoản bị khóa"
                                422 -> "Thông tin đăng nhập không hợp lệ"
                                500 -> "Lỗi máy chủ. Vui lòng thử lại sau"
                                else -> "Lỗi đăng nhập (${response.code()})"
                            }
                            Log.e(TAG, "❌ HTTP Error ${response.code()}: $errorMsg")
                            showError(errorMsg)
                        }
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "⏰ Socket timeout during login", e)
                showError("Kết nối bị timeout. Vui lòng kiểm tra mạng và thử lại.")
            } catch (e: UnknownHostException) {
                Log.e(TAG, "🌐 DNS resolution failed during login", e)
                showError("Không thể kết nối đến máy chủ. Kiểm tra kết nối internet.")
            } catch (e: SSLException) {
                Log.e(TAG, "🔒 SSL error during login", e)
                showError("Lỗi bảo mật kết nối. Vui lòng thử lại.")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Unexpected error during login", e)
                showError("Lỗi không xác định. Vui lòng thử lại.")
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
        binding.btnLogin.isEnabled = !show
        binding.btnGuest.isEnabled = !show
        
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