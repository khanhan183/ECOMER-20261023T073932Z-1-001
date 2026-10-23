package com.example.ecomer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ecomer.databinding.ActivityLoginBinding
import com.example.ecomer.repository.AuthRepository

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authRepository by lazy { AuthRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAutoLogin()

        binding.btnLogin.setOnClickListener {
            handleLogin()
        }
    }

    private fun checkAutoLogin() {
        if (authRepository.isUserLoggedIn()) {
            val currentUid = authRepository.getCurrentUserId() ?: ""
            showLoading(true)
            authRepository.getUserRole(
                uid = currentUid,
                onSuccess = { userRole ->
                    showLoading(false)
                    navigateToMain(isAdmin = userRole.isAdmin(), email = userRole.email)
                },
                onFailure = { exception ->
                    showLoading(false)
                    val email = authRepository.getCurrentUserEmail() ?: ""
                    Toast.makeText(this, "Lỗi đọc vai trò: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                    navigateToMain(isAdmin = false, email = email)
                }
            )
        }
    }

    private fun handleLogin() {
        val email = binding.edtEmail.text?.toString()?.trim() ?: ""
        val password = binding.edtPassword.text?.toString()?.trim() ?: ""

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.err_empty_email)
            return
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.err_empty_password)
            return
        } else {
            binding.tilPassword.error = null
        }

        showLoading(true)

        authRepository.login(
            email = email,
            password = password,
            onSuccess = { uid ->
                authRepository.getUserRole(
                    uid = uid,
                    onSuccess = { userRole ->
                        showLoading(false)
                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                        navigateToMain(isAdmin = userRole.isAdmin(), email = email)
                    },
                    onFailure = { exception ->
                        showLoading(false)
                        Toast.makeText(this, "Lỗi phân quyền Firestore: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                        navigateToMain(isAdmin = false, email = email)
                    }
                )
            },
            onFailure = { exception ->
                showLoading(false)
                val errorMessage = exception.localizedMessage ?: "Mật khẩu hoặc email không đúng."
                Toast.makeText(
                    this,
                    getString(R.string.err_login_failed) + errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.edtEmail.isEnabled = !isLoading
        binding.edtPassword.isEnabled = !isLoading
    }

    private fun navigateToMain(isAdmin: Boolean, email: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_IS_ADMIN, isAdmin)
            putExtra(EXTRA_USER_EMAIL, email)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_IS_ADMIN = "extra_is_admin"
        const val EXTRA_USER_EMAIL = "extra_user_email"
    }
}
