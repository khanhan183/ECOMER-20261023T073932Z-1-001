package com.example.ecomer.model

data class UserRole(
    val uid: String = "",
    val email: String = "",
    val role: String = "user"
) {
    fun isAdmin(): Boolean {
        return role.trim().equals("admin", ignoreCase = true) ||
                email.trim().lowercase().startsWith("admin")
    }
}
