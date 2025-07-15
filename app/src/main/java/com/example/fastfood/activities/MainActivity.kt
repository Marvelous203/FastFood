package com.example.fastfood.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.fastfood.R
import com.example.fastfood.databinding.ActivityMainBinding
import com.example.fastfood.utils.CartManager
import com.example.fastfood.activities.CartActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val cartManager by lazy { CartManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupCartFab()
        updateCartBadge()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun setupCartFab() {
        binding.fabCart.setOnClickListener {
            CartActivity.start(this)
        }
    }

    private fun updateCartBadge() {
        val itemCount = cartManager.getItemCount()
        binding.cartBadge.apply {
            visibility = if (itemCount > 0) View.VISIBLE else View.GONE
            text = itemCount.toString()
        }
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
    }
}
