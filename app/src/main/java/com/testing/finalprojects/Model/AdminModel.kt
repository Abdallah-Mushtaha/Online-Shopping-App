package com.testing.finalprojects.Model

import com.google.gson.annotations.SerializedName

data class Category(
    var id: Long,
    var name: String,
    var image: String = "drawable/trash.png"
)


data class Product(
    val id: String,
    var name: String,
    var description: String = "",
    var imageUrl: String = "",
    var price: Double,
    val rate: Float = 0.0f,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var category: String? = null,
    var locationName: String = "",
    val location: String? = null,
    val categoryId: Int,
    var quantity: Int = 1,
    val image: String,
    val totalOrdered: Int? = null
)

data class User(
    val id: String,
    val email: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    val phone: String?,
    val role: String
)


