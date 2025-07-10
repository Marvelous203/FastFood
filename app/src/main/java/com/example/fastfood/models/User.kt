package com.example.fastfood.models

import com.google.gson.annotations.SerializedName

data class User(
    val id: String,
    val email: String,
    @SerializedName("firstName")
    val firstName: String? = null,
    @SerializedName("lastName") 
    val lastName: String? = null,
    val phone: String? = null,
    val role: Role? = null,
    val status: Status? = null,
    val photo: Photo? = null,
    val provider: String? = null,
    val socialId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null
) {
    // Helper property for full name
    val fullName: String
        get() = when {
            firstName != null && lastName != null -> "$firstName $lastName"
            firstName != null -> firstName
            lastName != null -> lastName
            else -> email.substringBefore("@")
        }
    
    // Helper properties for easier access
    val roleName: String?
        get() = role?.name
    
    val statusName: String?
        get() = status?.name
    
    val photoUrl: String?
        get() = photo?.path
    
    val isActive: Boolean
        get() = status?.name?.lowercase() == "active"
}

data class Role(
    val id: String,
    val name: String
)

data class Status(
    val id: String,
    val name: String
)

data class Photo(
    val id: String,
    val path: String
)

// Additional request models for new endpoints
data class ConfirmEmailRequest(
    val hash: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val password: String,
    val hash: String
)

data class UpdateProfileRequest(
    @SerializedName("firstName")
    val firstName: String? = null,
    @SerializedName("lastName")
    val lastName: String? = null,
    val phone: String? = null,
    val photo: String? = null
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class SocialLoginRequest(
    val accessToken: String,
    @SerializedName("firstName")
    val firstName: String? = null,
    @SerializedName("lastName")
    val lastName: String? = null
) 