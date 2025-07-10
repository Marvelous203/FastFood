package com.example.fastfood.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.fastfood.R
import com.example.fastfood.activities.ProductDetailActivity
import com.example.fastfood.adapters.FoodAdapter
import com.example.fastfood.databinding.FragmentMenuBinding
import com.example.fastfood.models.CartItem
import com.example.fastfood.models.Food
import com.example.fastfood.network.ApiService
import com.example.fastfood.network.RetrofitClient
import com.example.fastfood.utils.CartManager
import com.example.fastfood.utils.NetworkHelper
import com.example.fastfood.utils.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class MenuFragment : Fragment() {

    private val TAG = "MenuFragment"
    private val gson = Gson()

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"

        fun newInstance(categoryId: String? = null): MenuFragment {
            return MenuFragment().apply {
                arguments = Bundle().apply {
                    categoryId?.let { putString(ARG_CATEGORY_ID, it) }
                }
            }
        }
    }

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var apiService: ApiService
    private lateinit var cartManager: CartManager
    private lateinit var foodAdapter: FoodAdapter
    private var categoryId: String? = null
    private var foods: List<Food> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryId = arguments?.getString(ARG_CATEGORY_ID)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        apiService = RetrofitClient.create(ApiService::class.java)
        cartManager = CartManager(requireContext())
        
        setupRecyclerView()
        setupSwipeRefresh()
        loadFoods()
    }
    
    private fun setupRecyclerView() {
        foodAdapter = FoodAdapter(
            foods = foods,
            onItemClick = { food ->
                ProductDetailActivity.start(requireContext(), food.id)
            },
            onAddToCartClick = { food ->
                addToCart(food)
            }
        )
        
        binding.rvFoods.apply {
            adapter = foodAdapter
            layoutManager = GridLayoutManager(context, 2)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadFoods()
        }
    }

    private fun addToCart(food: Food) {
        lifecycleScope.launch {
            try {
                val result = cartManager.addToCart(food.id, 1)
                when (result) {
                    is Result.Success -> {
                        showMessage(getString(R.string.add_to_cart_success))
                    }
                    is Result.Failure -> {
                        showError(result.exception.message ?: getString(R.string.error_add_to_cart))
                    }
                }
            } catch (e: Exception) {
                showError(getString(R.string.error_add_to_cart))
            }
        }
    }
    
    private fun loadFoods() {
        if (!NetworkHelper.isNetworkAvailable(requireContext())) {
            showEmptyState(
                show = true,
                icon = R.drawable.ic_no_internet,
                title = getString(R.string.no_internet),
                subtitle = getString(R.string.check_internet_connection)
            )
            return
        }
        
        showLoading(true)
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading foods, categoryId: $categoryId")
                val response = if (categoryId != null) {
                    apiService.getFoodsByCategory(categoryId = categoryId!!)
                } else {
                    apiService.getFoods()
                }
                
                Log.d(TAG, "Foods response: ${response.isSuccessful}, code: ${response.code()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    Log.d(TAG, "Foods response body: $responseBody")
                    
                    val dataObj = responseBody["data"]
                    if (dataObj != null) {
                        val dataJson = gson.toJson(dataObj)
                        val foodType = object : TypeToken<List<Food>>() {}.type
                        foods = gson.fromJson(dataJson, foodType)
                        
                        Log.d(TAG, "Found ${foods.size} foods")
                        
                        setupRecyclerView() // Recreate adapter with new foods
                        if (foods.isEmpty()) {
                            showEmptyState(
                                show = true,
                                icon = R.drawable.ic_restaurant_menu,
                                title = if (categoryId != null) {
                                    getString(R.string.no_foods_in_category)
                                } else {
                                    getString(R.string.no_foods_available)
                                },
                                subtitle = getString(R.string.check_back_later)
                            )
                        } else {
                            showEmptyState(show = false)
                        }
                    } else {
                        Log.e(TAG, "Data field is null in response")
                        showEmptyState(
                            show = true,
                            icon = R.drawable.ic_error,
                            title = getString(R.string.load_data_failed),
                            subtitle = getString(R.string.try_again_later)
                        )
                    }
                } else {
                    Log.e(TAG, "Foods request failed: ${response.code()}")
                    showEmptyState(
                        show = true,
                        icon = R.drawable.ic_error,
                        title = getString(R.string.load_data_failed),
                        subtitle = getString(R.string.try_again_later)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Error loading foods: ${e.message}")
                showEmptyState(
                    show = true,
                    icon = R.drawable.ic_error,
                    title = getString(R.string.error_network),
                    subtitle = getString(R.string.try_again_later)
                )
            } finally {
                showLoading(false)
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvFoods.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(
        show: Boolean,
        icon: Int? = null,
        title: String? = null,
        subtitle: String? = null
    ) {
        binding.layoutEmptyState.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvFoods.visibility = if (show) View.GONE else View.VISIBLE
        
        if (show) {
            icon?.let { binding.ivEmptyIcon.setImageResource(it) }
            title?.let { binding.tvEmptyState.text = it }
            subtitle?.let { binding.tvEmptyStateSubtitle.text = it }
            
            binding.btnRetry.setOnClickListener {
                loadFoods()
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 