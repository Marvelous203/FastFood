package com.example.fastfood.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fastfood.databinding.FragmentOrdersBinding
import com.example.fastfood.utils.PreferencesManager

class OrdersFragment : Fragment() {
    
    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        setupViews()
    }
    
    private fun setupViews() {
        // Check if user is logged in
        if (preferencesManager.isLoggedIn) {
            // TODO: Load orders from API
            showEmptyState()
        } else {
            showLoginRequired()
        }
    }
    
    private fun showEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.layoutLoginRequired.visibility = View.GONE
    }
    
    private fun showLoginRequired() {
        binding.layoutLoginRequired.visibility = View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE
        
        binding.btnLogin.setOnClickListener {
            // TODO: Navigate to login
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 