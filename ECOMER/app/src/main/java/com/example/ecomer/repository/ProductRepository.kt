package com.example.ecomer.repository

import com.example.ecomer.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProductRepository {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val productsCollection = firestore.collection("products")

    fun addProduct(
        product: Product,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        productsCollection.add(product.toMap())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun updateProduct(
        product: Product,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (product.id.isEmpty()) {
            onFailure(IllegalArgumentException("Document ID không hợp lệ."))
            return
        }

        productsCollection.document(product.id)
            .set(product.toMap())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun deleteProduct(
        productId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (productId.isEmpty()) {
            onFailure(IllegalArgumentException("Document ID không hợp lệ."))
            return
        }

        productsCollection.document(productId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun listenToProducts(
        onDataChanged: (List<Product>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return productsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val productList = snapshot.documents.mapNotNull { doc ->
                    val prod = doc.toObject(Product::class.java)
                    prod?.apply { id = doc.id }
                }
                onDataChanged(productList)
            }
        }
    }
}
