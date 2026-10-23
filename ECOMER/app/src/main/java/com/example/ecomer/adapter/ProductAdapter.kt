package com.example.ecomer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecomer.R
import com.example.ecomer.databinding.ItemProductBinding
import com.example.ecomer.model.Product
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private var productList: List<Product> = emptyList(),
    private var isAdmin: Boolean = true,
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    fun updateRole(adminRole: Boolean) {
        this.isAdmin = adminRole
        notifyDataSetChanged()
    }

    fun updateData(newProducts: List<Product>, adminRole: Boolean = isAdmin) {
        this.productList = newProducts
        this.isAdmin = adminRole
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount(): Int = productList.size

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.tvProductName.text = product.name
            binding.tvProductCategory.text = binding.root.context.getString(
                R.string.product_category_hint
            ) + ": " + product.category

            binding.tvProductPrice.text = formatVndPrice(product.price)

            // Glide image loading with placeholder & error handling
            Glide.with(binding.root.context)
                .load(product.image.trim())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_broken_image)
                .centerCrop()
                .into(binding.imgProduct)

            // Admin privileges check for actions
            if (isAdmin) {
                binding.layoutActions.visibility = View.VISIBLE
                binding.btnEditProduct.setOnClickListener { onEditClick(product) }
                binding.btnDeleteProduct.setOnClickListener { onDeleteClick(product) }
            } else {
                binding.layoutActions.visibility = View.GONE
            }
        }
    }

    private fun formatVndPrice(price: Double): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            val formatted = formatter.format(price)
            formatted.replace("₫", "đ").replace("VND", "đ").trim()
        } catch (e: Exception) {
            "${price.toLong()} đ"
        }
    }
}
