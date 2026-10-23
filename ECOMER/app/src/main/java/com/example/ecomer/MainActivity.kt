package com.example.ecomer

import android.content.Intent
import android.os.Bundle
import android.webkit.URLUtil
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.ecomer.adapter.ProductAdapter
import com.example.ecomer.databinding.ActivityMainBinding
import com.example.ecomer.model.Product
import com.example.ecomer.repository.AuthRepository
import com.example.ecomer.repository.ProductRepository
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepository by lazy { AuthRepository() }
    private val productRepository by lazy { ProductRepository() }

    private lateinit var productAdapter: ProductAdapter
    private var listenerRegistration: ListenerRegistration? = null

    private var isAdmin: Boolean = false
    private var editingProduct: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUserRole()
        setupRecyclerView()
        setupListeners()
        loadProductsRealtime()
    }

    private fun initUserRole() {
        val email = intent.getStringExtra(LoginActivity.EXTRA_USER_EMAIL)
            ?: authRepository.getCurrentUserEmail() ?: ""

        val intentIsAdmin = intent.getBooleanExtra(LoginActivity.EXTRA_IS_ADMIN, false)
        isAdmin = intentIsAdmin || isEmailOrRoleAdmin(email, "")

        updateUserUi(email, isAdmin)

        // Fetch Firestore role asynchronously
        val currentUid = authRepository.getCurrentUserId() ?: ""
        if (currentUid.isNotEmpty()) {
            authRepository.getUserRole(
                uid = currentUid,
                onSuccess = { userRole ->
                    isAdmin = userRole.isAdmin() || isEmailOrRoleAdmin(userRole.email, userRole.role)
                    updateUserUi(userRole.email.ifEmpty { email }, isAdmin)
                },
                onFailure = {
                    // Fallback to email based admin check if Firestore query fails
                    isAdmin = isEmailOrRoleAdmin(email, "")
                    updateUserUi(email, isAdmin)
                }
            )
        }
    }

    private fun isEmailOrRoleAdmin(email: String, role: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val cleanRole = role.trim().lowercase()
        return cleanRole == "admin" || cleanEmail.startsWith("admin") || cleanEmail.contains("admin")
    }

    private fun updateUserUi(email: String, adminRole: Boolean) {
        val roleStr = if (adminRole) getString(R.string.role_admin) else getString(R.string.role_user)
        binding.tvUserAccount.text = "$email ($roleStr)"

        if (adminRole) {
            binding.cardProductForm.visibility = View.VISIBLE
        } else {
            binding.cardProductForm.visibility = View.GONE
        }

        if (::productAdapter.isInitialized) {
            productAdapter.updateRole(adminRole)
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            productList = emptyList(),
            isAdmin = isAdmin,
            onEditClick = { product ->
                if (isAdmin) {
                    enterEditMode(product)
                } else {
                    Toast.makeText(this, R.string.no_admin_permission, Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteClick = { product ->
                if (isAdmin) {
                    showDeleteConfirmDialog(product)
                } else {
                    Toast.makeText(this, R.string.no_admin_permission, Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = productAdapter
        }
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            handleLogout()
        }

        binding.btnSeedData.setOnClickListener {
            seedSampleData()
        }

        binding.btnPreviewImage.setOnClickListener {
            previewImage()
        }

        binding.btnSaveProduct.setOnClickListener {
            handleSaveProduct()
        }

        binding.btnCancelEdit.setOnClickListener {
            resetForm()
        }
    }

    private fun seedSampleData() {
        val samples = listOf(
            Product(
                name = "Áo Thun Nam Cotton Co Dãn",
                category = "Thời trang",
                price = 150000.0,
                image = "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500"
            ),
            Product(
                name = "Giày Sneaker Thể Thao Nam",
                category = "Giày dép",
                price = 450000.0,
                image = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500"
            ),
            Product(
                name = "Điện Thoại Thông Minh Pro",
                category = "Điện tử",
                price = 8990000.0,
                image = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500"
            ),
            Product(
                name = "Tai Nghe Bluetooth Không Dây",
                category = "Phụ kiện",
                price = 320000.0,
                image = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500"
            ),
            Product(
                name = "Đồng Hồ Nam Thể Thao",
                category = "Trang sức",
                price = 1250000.0,
                image = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500"
            )
        )

        var count = 0
        binding.btnSeedData.isEnabled = false
        Toast.makeText(this, "Đang thêm 5 sản phẩm mẫu vào Firestore...", Toast.LENGTH_SHORT).show()

        samples.forEach { product ->
            productRepository.addProduct(
                product = product,
                onSuccess = {
                    count++
                    if (count == samples.size) {
                        binding.btnSeedData.isEnabled = true
                        Toast.makeText(this, "Đã thêm thành công 5 sản phẩm mẫu!", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { exception ->
                    binding.btnSeedData.isEnabled = true
                    Toast.makeText(this, "Lỗi thêm dữ liệu: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun previewImage() {
        val url = binding.edtProductImageUrl.text?.toString()?.trim() ?: ""
        if (url.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập URL hình ảnh trước khi xem thử", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidUrl(url)) {
            Toast.makeText(this, "URL hình ảnh không đúng định dạng (phải bắt đầu bằng http:// hoặc https://)", Toast.LENGTH_SHORT).show()
            return
        }

        binding.tvPreviewLabel.visibility = View.VISIBLE
        binding.imgPreview.visibility = View.VISIBLE

        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_broken_image)
            .centerCrop()
            .into(binding.imgPreview)
    }

    private fun handleSaveProduct() {
        if (!isAdmin) {
            Toast.makeText(this, R.string.no_admin_permission, Toast.LENGTH_SHORT).show()
            return
        }

        val name = binding.edtProductName.text?.toString()?.trim() ?: ""
        val category = binding.edtProductCategory.text?.toString()?.trim() ?: ""
        val priceStr = binding.edtProductPrice.text?.toString()?.trim() ?: ""
        val imageUrl = binding.edtProductImageUrl.text?.toString()?.trim() ?: ""

        if (name.isEmpty()) {
            binding.edtProductName.error = "Vui lòng nhập tên sản phẩm"
            binding.edtProductName.requestFocus()
            return
        }

        if (category.isEmpty()) {
            binding.edtProductCategory.error = "Vui lòng nhập loại sản phẩm"
            binding.edtProductCategory.requestFocus()
            return
        }

        if (priceStr.isEmpty()) {
            binding.edtProductPrice.error = "Vui lòng nhập giá sản phẩm"
            binding.edtProductPrice.requestFocus()
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null || price < 0) {
            binding.edtProductPrice.error = "Giá sản phẩm phải là số hợp lệ và không âm"
            binding.edtProductPrice.requestFocus()
            return
        }

        if (imageUrl.isEmpty()) {
            binding.edtProductImageUrl.error = "Vui lòng nhập URL hình ảnh"
            binding.edtProductImageUrl.requestFocus()
            return
        }

        if (!isValidUrl(imageUrl)) {
            binding.edtProductImageUrl.error = "URL không hợp lệ. Phải có định dạng http:// hoặc https://"
            binding.edtProductImageUrl.requestFocus()
            return
        }

        val productToSave = Product(
            id = editingProduct?.id ?: "",
            name = name,
            category = category,
            price = price,
            image = imageUrl
        )

        binding.btnSaveProduct.isEnabled = false

        if (editingProduct != null) {
            // Edit existing product
            productRepository.updateProduct(
                product = productToSave,
                onSuccess = {
                    binding.btnSaveProduct.isEnabled = true
                    Toast.makeText(this, "Cập nhật sản phẩm thành công!", Toast.LENGTH_SHORT).show()
                    resetForm()
                },
                onFailure = { exception ->
                    binding.btnSaveProduct.isEnabled = true
                    Toast.makeText(this, "Lỗi cập nhật: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            // Add new product
            productRepository.addProduct(
                product = productToSave,
                onSuccess = {
                    binding.btnSaveProduct.isEnabled = true
                    Toast.makeText(this, "Thêm sản phẩm thành công!", Toast.LENGTH_SHORT).show()
                    resetForm()
                },
                onFailure = { exception ->
                    binding.btnSaveProduct.isEnabled = true
                    Toast.makeText(this, "Lỗi thêm sản phẩm: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun enterEditMode(product: Product) {
        editingProduct = product
        binding.tvFormTitle.text = "Sửa sản phẩm"
        binding.btnSaveProduct.text = getString(R.string.btn_save_changes)
        binding.btnCancelEdit.visibility = View.VISIBLE

        binding.edtProductName.setText(product.name)
        binding.edtProductCategory.setText(product.category)
        binding.edtProductPrice.setText(product.price.toString())
        binding.edtProductImageUrl.setText(product.image)

        previewImage()
        binding.cardProductForm.requestFocus()
    }

    private fun resetForm() {
        editingProduct = null
        binding.tvFormTitle.text = getString(R.string.btn_add_product)
        binding.btnSaveProduct.text = getString(R.string.btn_add_product)
        binding.btnCancelEdit.visibility = View.GONE

        binding.edtProductName.text = null
        binding.edtProductCategory.text = null
        binding.edtProductPrice.text = null
        binding.edtProductImageUrl.text = null

        binding.edtProductName.error = null
        binding.edtProductCategory.error = null
        binding.edtProductPrice.error = null
        binding.edtProductImageUrl.error = null

        binding.tvPreviewLabel.visibility = View.GONE
        binding.imgPreview.visibility = View.GONE
    }

    private fun showDeleteConfirmDialog(product: Product) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_msg)
            .setPositiveButton(R.string.btn_delete) { dialog, _ ->
                dialog.dismiss()
                deleteProduct(product)
            }
            .setNegativeButton(R.string.btn_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteProduct(product: Product) {
        productRepository.deleteProduct(
            productId = product.id,
            onSuccess = {
                Toast.makeText(this, "Xóa sản phẩm thành công!", Toast.LENGTH_SHORT).show()
                if (editingProduct?.id == product.id) {
                    resetForm()
                }
            },
            onFailure = { exception ->
                Toast.makeText(this, "Lỗi xóa sản phẩm: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun loadProductsRealtime() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyList.visibility = View.GONE

        listenerRegistration?.remove()
        listenerRegistration = productRepository.listenToProducts(
            onDataChanged = { products ->
                binding.progressBar.visibility = View.GONE
                if (products.isEmpty()) {
                    binding.tvEmptyList.visibility = View.VISIBLE
                } else {
                    binding.tvEmptyList.visibility = View.GONE
                }
                productAdapter.updateData(products, isAdmin)
            },
            onError = { exception ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Lỗi tải danh sách: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun handleLogout() {
        authRepository.logout()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun isValidUrl(url: String): Boolean {
        return URLUtil.isValidUrl(url) && (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true))
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
