package com.example.ecomer.repository

import com.example.ecomer.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class AuthRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun login(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: ""
                onSuccess(uid)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun getUserRole(
        uid: String,
        onSuccess: (UserRole) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentEmail = auth.currentUser?.email ?: ""
        if (uid.isEmpty()) {
            onSuccess(UserRole(uid = "", email = currentEmail, role = "user"))
            return
        }

        // Try reading from SERVER first to avoid Firestore offline cache serving old empty document
        firestore.collection("users").document(uid).get(Source.SERVER)
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val rawRole = documentSnapshot.getString("role") ?: "user"
                    val docEmail = documentSnapshot.getString("email") ?: currentEmail
                    onSuccess(UserRole(uid = uid, email = docEmail, role = rawRole.trim()))
                } else {
                    onSuccess(UserRole(uid = uid, email = currentEmail, role = "user"))
                }
            }
            .addOnFailureListener {
                // Fallback to default cache/network query if SERVER query fails
                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val rawRole = documentSnapshot.getString("role") ?: "user"
                            val docEmail = documentSnapshot.getString("email") ?: currentEmail
                            onSuccess(UserRole(uid = uid, email = docEmail, role = rawRole.trim()))
                        } else {
                            onSuccess(UserRole(uid = uid, email = currentEmail, role = "user"))
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Even if query fails, provide UserRole with current email so fallback check can work
                        onSuccess(UserRole(uid = uid, email = currentEmail, role = "user"))
                    }
            }
    }

    fun logout() {
        auth.signOut()
    }
}
