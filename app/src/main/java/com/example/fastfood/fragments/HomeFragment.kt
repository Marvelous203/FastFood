package com.example.fastfood.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import com.example.fastfood.R
import com.example.fastfood.activities.ProductDetailActivity
import com.example.fastfood.activities.SearchActivity
import com.example.fastfood.adapters.CategoryAdapter
import com.example.fastfood.adapters.FoodAdapter
import com.example.fastfood.databinding.FragmentHomeBinding
import com.example.fastfood.models.Category
import com.example.fastfood.models.Food
import com.example.fastfood.models.CartItem
import com.example.fastfood.network.ApiService
import com.example.fastfood.network.RetrofitClient
import com.example.fastfood.utils.PreferencesManager
import com.example.fastfood.utils.CartManager
import com.example.fastfood.utils.NetworkHelper
import com.example.fastfood.utils.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.util.Calendar
import android.content.Intent

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val TAG = "HomeFragment"
    private val gson = Gson()

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var apiService: ApiService
    private lateinit var cartManager: CartManager
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var popularFoodAdapter: FoodAdapter
    
    private var categories: List<Category> = emptyList()
    private var popularFoods: List<Food> = emptyList()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            Log.d(TAG, "onViewCreated: Initializing HomeFragment")
            initializeComponents()
            setupViews()
            setupRecyclerViews()
            setupClickListeners()
            loadData()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing HomeFragment: ${e.message}")
            showError(getString(R.string.init_error, e.message))
        }
    }

    private fun initializeComponents() {
        preferencesManager = PreferencesManager(requireContext())
        apiService = RetrofitClient.create(ApiService::class.java)
        cartManager = CartManager(requireContext())
    }
    
    private fun setupViews() {
        val calendar = Calendar.getInstance()
        val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> getString(R.string.good_morning)
            in 12..17 -> getString(R.string.good_afternoon)
            else -> getString(R.string.good_evening)
        }
        // Greeting text will be used when we add the greeting TextView
    }
    
    private fun setupRecyclerViews() {
        setupCategoryAdapter()
        setupPopularFoodAdapter()
    }

    private fun setupCategoryAdapter() {
        categoryAdapter = CategoryAdapter(
            onItemClick = { 
                navigateToMenu()
            }
        )
        binding.categoriesRecyclerView.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        categoryAdapter.submitList(categories)
    }

    private fun setupPopularFoodAdapter() {
        popularFoodAdapter = FoodAdapter(
            foods = popularFoods,
            onItemClick = { food ->
                ProductDetailActivity.start(requireContext(), food.id)
            },
            onAddToCartClick = { food ->
                addToCart(food)
            }
        )
        binding.popularFoodsRecyclerView.apply {
            adapter = popularFoodAdapter
            layoutManager = GridLayoutManager(context, 2)
        }
    }
    
    private fun setupClickListeners() {
        with(binding) {
            viewAllCategories.setOnClickListener {
                navigateToMenu()
            }
            
            viewAllPopular.setOnClickListener {
                navigateToMenu()
            }
            
            searchEditText.setOnClickListener {
                navigateToSearch()
            }
        }
    }

    private fun navigateToMenu() {
        findNavController().navigate(R.id.action_navigation_home_to_navigation_menu)
    }

    private fun navigateToSearch() {
        startActivity(Intent(requireContext(), SearchActivity::class.java))
    }

    private fun addToCart(food: Food) {
        lifecycleScope.launch {
            try {
                val result = cartManager.addToCart(food.id, 1)
                when (result) {
                    is Result.Success<CartItem> -> {
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
    
    private fun loadData() {
        if (!NetworkHelper.isNetworkAvailable(requireContext())) {
            showError(getString(R.string.no_internet))
            return
        }
        
        showLoading(true)
        Log.d(TAG, "Loading data from API...")
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                loadCategories()
                loadPopularFoods()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading data: ${e.message}")
                showError(getString(R.string.error_loading_data))
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun loadCategories() {
        Log.d(TAG, "Fetching categories...")
        val response = apiService.getCategories()
        Log.d(TAG, "Categories response: ${response.isSuccessful}, code: ${response.code()}")
        
        if (response.isSuccessful && response.body() != null) {
            val responseBody = response.body()!!
            val prettyJson = gson.toJson(responseBody)
            Log.d(TAG, "Categories response body: $prettyJson")
            
            val dataObj = responseBody["data"]
            if (dataObj != null) {
                val dataJson = gson.toJson(dataObj)
                Log.d(TAG, "Categories data JSON: $dataJson")
                
                val categoryType = object : TypeToken<List<Category>>() {}.type
                categories = gson.fromJson(dataJson, categoryType)
                
                Log.d(TAG, "Found ${categories.size} categories")
                categories.forEachIndexed { index, category ->
                    Log.d(TAG, "Category $index: ${category.name}")
                }
                
                categoryAdapter.submitList(categories)
            }
        } else {
            Log.e(TAG, "Error fetching categories: ${response.code()}")
            showError(getString(R.string.error_loading_categories))
        }
    }

    private suspend fun loadPopularFoods() {
        val sortJson = gson.toJson(listOf(mapOf("field" to "rating", "order" to "DESC")))
        val response = apiService.getFoods(limit = 10, sort = sortJson)
        
        if (response.isSuccessful && response.body() != null) {
            val responseBody = response.body()!!
            val dataObj = responseBody["data"]
            if (dataObj != null) {
                val dataJson = gson.toJson(dataObj)
                val foodType = object : TypeToken<List<Food>>() {}.type
                popularFoods = gson.fromJson(dataJson, foodType)
                setupPopularFoodAdapter()
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
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