package com.example.fastfood.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.fastfood.activities.AuthActivity
import com.example.fastfood.activities.OrderDetailActivity
import com.example.fastfood.adapters.OrderAdapter
import com.example.fastfood.databinding.FragmentOrdersBinding
import com.example.fastfood.models.Order
import com.example.fastfood.utils.PreferencesManager
import com.example.fastfood.viewmodels.OrderViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferencesManager: PreferencesManager
    private val orderViewModel: OrderViewModel by viewModels()
    private lateinit var orderAdapter: OrderAdapter

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
        setupObservers()

        if (preferencesManager.isLoggedIn) {
            setupRecyclerView()
            setupFilterTabs()
            setupSwipeRefresh()
            // Load orders when fragment is created
            orderViewModel.loadOrders()
        }
    }

    private fun setupViews() {
        if (preferencesManager.isLoggedIn) {
            binding.rvOrders.visibility = View.VISIBLE
            binding.layoutLoginRequired.visibility = View.GONE
        } else {
            showLoginRequired()
        }

        binding.btnStartOrdering.setOnClickListener {
            // Navigate to home fragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(android.R.id.content,
                    requireActivity().supportFragmentManager.fragments.first { it.tag == "HOME" })
                .commit()
        }
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(
            onOrderClick = { order -> navigateToOrderDetail(order) },
            onCancelOrder = { order -> showCancelOrderDialog(order) },
            onReorder = { order -> orderViewModel.reorder(order) }
        )

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter

            // Add scroll listener for pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                        orderViewModel.loadMoreOrders()
                    }
                }
            })
        }
    }

    private fun setupFilterTabs() {
        // Add filter tabs programmatically
        OrderViewModel.OrderFilter.values().forEach { filter ->
            val tab = binding.tabLayoutOrderFilter.newTab()
            tab.text = filter.displayName
            tab.tag = filter
            binding.tabLayoutOrderFilter.addTab(tab)
        }

        binding.tabLayoutOrderFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val filter = tab?.tag as? OrderViewModel.OrderFilter ?: OrderViewModel.OrderFilter.ALL
                orderViewModel.filterOrders(filter)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout?.setOnRefreshListener {
            orderViewModel.loadOrders(refresh = true)
        }
    }

    private fun setupObservers() {
        orderViewModel.filteredOrders.observe(viewLifecycleOwner) { orders ->
            android.util.Log.d("OrdersFragment", "Received ${orders?.size ?: 0} filtered orders")
            orderAdapter.submitList(orders)
        }

        orderViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout?.isRefreshing = isLoading
        }

        orderViewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            android.util.Log.d("OrdersFragment", "isEmpty changed to: $isEmpty")
            if (isEmpty) {
                showEmptyState()
            } else {
                hideEmptyState()
            }
        }

        orderViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                orderViewModel.clearMessages()
            }
        }

        orderViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                orderViewModel.clearMessages()
            }
        }

        orderViewModel.currentFilter.observe(viewLifecycleOwner) { filter ->
            // Update selected tab
            val tabIndex = OrderViewModel.OrderFilter.values().indexOf(filter)
            if (tabIndex >= 0 && tabIndex < binding.tabLayoutOrderFilter.tabCount) {
                binding.tabLayoutOrderFilter.getTabAt(tabIndex)?.select()
            }
        }
    }

    private fun showEmptyState() {
        android.util.Log.d("OrdersFragment", "Showing empty state")
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.rvOrders.visibility = View.GONE
    }

    private fun hideEmptyState() {
        android.util.Log.d("OrdersFragment", "Hiding empty state - showing orders")
        binding.layoutEmptyState.visibility = View.GONE
        binding.rvOrders.visibility = View.VISIBLE
    }

    private fun showLoginRequired() {
        binding.layoutLoginRequired.visibility = View.VISIBLE
        binding.rvOrders.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE

        binding.btnLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), AuthActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToOrderDetail(order: Order) {
        val intent = Intent(requireContext(), OrderDetailActivity::class.java)
        intent.putExtra("order_id", order.id)
        startActivity(intent)
    }

    private fun showCancelOrderDialog(order: Order) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xác nhận hủy đơn hàng")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng #${(order.id ?: "UNKNOWN").take(8).uppercase()}?")
            .setPositiveButton("Hủy đơn") { _, _ ->
                orderViewModel.cancelOrder(order)
            }
            .setNegativeButton("Không", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (preferencesManager.isLoggedIn) {
            orderViewModel.loadOrders(refresh = true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
