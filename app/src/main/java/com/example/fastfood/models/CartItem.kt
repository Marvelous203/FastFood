package com.example.fastfood.models

data class CartItem(
    val foodId: String,
    var quantity: Int,
    var price: Double = 0.0,  // Thêm field để lưu giá sản phẩm
    var name: String = "",    // Thêm field để lưu tên sản phẩm
    var image: String = ""    // Thêm field để lưu ảnh sản phẩm
)
