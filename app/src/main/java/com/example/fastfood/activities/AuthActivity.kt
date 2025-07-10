package com.example.fastfood.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fastfood.R
import com.example.fastfood.databinding.ActivityAuthBinding
import com.example.fastfood.fragments.LoginFragment
import com.example.fastfood.fragments.RegisterFragment
import com.google.android.material.tabs.TabLayoutMediator

class AuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAuthBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewPager()
    }
    
    private fun setupViewPager() {
        val adapter = AuthPagerAdapter(this)
        binding.viewPager.adapter = adapter
        
        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.login)
                1 -> getString(R.string.register)
                else -> ""
            }
        }.attach()
    }
    
    fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    fun switchToRegister() {
        binding.viewPager.currentItem = 1
    }
    
    fun switchToLogin() {
        binding.viewPager.currentItem = 0
    }
    
    private class AuthPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> LoginFragment()
                1 -> RegisterFragment()
                else -> LoginFragment()
            }
        }
    }
} 