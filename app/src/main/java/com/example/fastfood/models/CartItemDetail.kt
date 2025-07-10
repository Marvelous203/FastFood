package com.example.fastfood.models

data class CartItemDetail(
    val foodId: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String?
) {
    fun toCartItem(): CartItem {
        return CartItem(
            foodId = foodId,
            quantity = quantity
        )
    }

    companion object {
        fun fromCartItem(cartItem: CartItem, name: String, price: Double, imageUrl: String?): CartItemDetail {
            return CartItemDetail(
                foodId = cartItem.foodId,
                name = name,
                price = price,
                quantity = cartItem.quantity,
                imageUrl = imageUrl
            )
        }
    }
} 