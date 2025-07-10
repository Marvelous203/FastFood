package com.example.fastfood.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.fastfood.R
import com.example.fastfood.adapters.FoodAdapter
import com.example.fastfood.databinding.ActivitySearchBinding
import com.example.fastfood.models.Food
import com.example.fastfood.models.CartItem
import com.example.fastfood.network.ApiService
import com.example.fastfood.network.RetrofitClient
import com.example.fastfood.utils.NetworkHelper
import com.example.fastfood.utils.CartManager
import com.example.fastfood.utils.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.fastfood.activities.ProductDetailActivity

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var apiService: ApiService
    private lateinit var foodAdapter: FoodAdapter
    private lateinit var cartManager: CartManager
    private var searchJob: Job? = null
    private val gson = Gson()
    private var currentFoods: List<Food> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiService = RetrofitClient.create(ApiService::class.java)
        cartManager = CartManager(this)
        setupRecyclerView()
        setupSearchView()
    }

    private fun setupRecyclerView() {
        foodAdapter = FoodAdapter(
            foods = currentFoods,
            onItemClick = { food ->
                ProductDetailActivity.start(this, food.id)
            },
            onAddToCartClick = { food ->
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
        )

        binding.rvSearchResults.apply {
            adapter = foodAdapter
            layoutManager = GridLayoutManager(this@SearchActivity, 2)
        }
    }

    private fun setupSearchView() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Search input with debounce
        binding.etSearch.addTextChangedListener { text ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(300) // Debounce typing
                text?.toString()?.let { query ->
                    if (query.isNotEmpty()) {
                        searchFoods(query)
                    } else {
                        updateFoodList(emptyList())
                        showEmptyState(true)
                    }
                }
            }
        }
    }

    private fun searchFoods(query: String) {
        if (!NetworkHelper.isNetworkAvailable(this)) {
            showError(getString(R.string.no_internet))
            return
        }

        showLoading(true)
        showEmptyState(false)

        lifecycleScope.launch {
            try {
                // Use getFoods with name filter
                val response = apiService.getFoods(
                    filters = "name:$query", // Filter by name containing query
                    limit = 20 // Limit results
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    val dataObj = responseBody["data"]
                    if (dataObj != null) {
                        val dataJson = gson.toJson(dataObj)
                        val foodType = object : TypeToken<List<Food>>() {}.type
                        val foods: List<Food> = gson.fromJson(dataJson, foodType)
                        updateFoodList(foods)
                        showEmptyState(foods.isEmpty())
                    } else {
                        updateFoodList(emptyList())
                        showEmptyState(true)
                    }
                } else {
                    showError(getString(R.string.error_loading_data))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError(getString(R.string.error_loading_data))
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateFoodList(foods: List<Food>) {
        currentFoods = foods
        foodAdapter = FoodAdapter(
            foods = currentFoods,
            onItemClick = { food ->
                ProductDetailActivity.start(this, food.id)
            },
            onAddToCartClick = { food ->
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
        )
        binding.rvSearchResults.adapter = foodAdapter
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        showMessage(message)
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showEmptyState(show: Boolean) {
        binding.layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvSearchResults.visibility = if (show) View.GONE else View.VISIBLE
    }
}
