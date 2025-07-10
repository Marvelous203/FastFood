package com.example.fastfood.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fastfood.R
import com.example.fastfood.databinding.ActivityMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var binding: ActivityMapBinding
    private lateinit var mMap: GoogleMap
    
    // Store location coordinates (example: Ho Chi Minh City)
    private val storeLocation = LatLng(10.8231, 106.6297)
    private val storeName = "FastFood Restaurant"
    private val storeAddress = "123 Nguyễn Huệ, Quận 1, TP.HCM"
    private val storePhone = "0123-456-789"
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MapActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupMapFragment()
        setupStoreInfo()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Vị trí cửa hàng"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupMapFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    
    private fun setupStoreInfo() {
        binding.tvStoreName.text = storeName
        binding.tvStoreAddress.text = storeAddress
        binding.tvStorePhone.text = storePhone
    }
    
    private fun setupClickListeners() {
        binding.btnDirections.setOnClickListener {
            openDirections()
        }
        
        binding.btnCall.setOnClickListener {
            callStore()
        }
        
        binding.btnShare.setOnClickListener {
            shareLocation()
        }
    }
    
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        // Add marker for store
        mMap.addMarker(
            MarkerOptions()
                .position(storeLocation)
                .title(storeName)
                .snippet(storeAddress)
        )
        
        // Move camera to store location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(storeLocation, 16f))
        
        // Enable zoom controls
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        
        // Set map type
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        
        // Set marker click listener
        mMap.setOnMarkerClickListener { marker ->
            if (marker.position == storeLocation) {
                showStoreDetails()
            }
            false
        }
    }
    
    private fun openDirections() {
        try {
            val uri = Uri.parse("google.navigation:q=${storeLocation.latitude},${storeLocation.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback to web version
                val webUri = Uri.parse("https://maps.google.com/maps?daddr=${storeLocation.latitude},${storeLocation.longitude}")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể mở chỉ đường", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun callStore() {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$storePhone")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể thực hiện cuộc gọi", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareLocation() {
        val shareText = """
            🍔 $storeName
            📍 $storeAddress
            📞 $storePhone
            
            Xem vị trí trên bản đồ: https://maps.google.com/maps?q=${storeLocation.latitude},${storeLocation.longitude}
        """.trimIndent()
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Vị trí $storeName")
        }
        
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ vị trí"))
    }
    
    private fun showStoreDetails() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(storeName)
            .setMessage("$storeAddress\n\nGiờ mở cửa:\nThứ 2 - CN: 8:00 - 22:00\n\nLiên hệ: $storePhone")
            .setPositiveButton("Gọi ngay") { _, _ ->
                callStore()
            }
            .setNegativeButton("Đóng", null)
            .setNeutralButton("Chỉ đường") { _, _ ->
                openDirections()
            }
            .show()
    }
}