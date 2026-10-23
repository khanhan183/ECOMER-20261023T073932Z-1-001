package com.example.ecomer.model

import com.google.firebase.firestore.Exclude

data class Product(
    @get:Exclude var id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val image: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "category" to category,
            "price" to price,
            "image" to image
        )
    }
}
