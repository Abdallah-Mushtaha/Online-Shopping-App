package com.example.onlineshop.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.testing.finalprojects.Model.Product
import com.testing.finalprojects.R
import com.testing.finalprojects.databinding.ItemProductBinding

enum class ButtonMode {
    ADMIN, CLIENT_HOME, CLIENT_CART, ADMIN_MANAGE, NONE
}

class ProductAdapter(
    private val context: Context,
    private var productList: List<Product>,
    internal var onButtonClick: (Product) -> Unit,
    private val buttonMode: ButtonMode = ButtonMode.CLIENT_HOME,
    private val showTotalOrdered: Boolean = false  // <--- إظهار totalOrdered فقط عند الحاجة
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    var onItemClick: ((Product) -> Unit)? = null
    var onQuantityChanged: ((Product) -> Unit)? = null

    fun updateList(newList: List<Product>) {
        productList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]

        holder.binding.tvName.text = product.name
        holder.binding.tvPrice.text = "${product.price}$"
        holder.binding.tvRate.text = "★ ${product.rate}"

        // ----------------------------------------------------------------------------------
        // عرض totalOrdered فقط إذا showTotalOrdered true
        if (showTotalOrdered && product.totalOrdered != null) {
            holder.binding.tvProductQuantity.visibility = View.VISIBLE
            holder.binding.tvProductQuantity.text = "Ordered: ${product.totalOrdered}"
        } else {
            // حسب ButtonMode أظهر الكمية أو اخفيها للمنتجات العادية
            if (buttonMode == ButtonMode.CLIENT_CART) {
                holder.binding.tvProductQuantity.visibility = View.GONE
            } else if (buttonMode == ButtonMode.NONE) {
                holder.binding.tvProductQuantity.visibility = View.VISIBLE
                holder.binding.tvProductQuantity.text = "Quantity: ${product.quantity}"
            } else {
                holder.binding.tvProductQuantity.visibility = View.GONE
            }
        }
        // ----------------------------------------------------------------------------------

        holder.binding.tvDescription.text = product.description ?: ""
        holder.binding.tvViewLocation.text = product.locationName

        // تحميل الصورة: رابط كامل أو نسبي
        val fullImageUrl = product.imageUrl ?: ""
        Glide.with(context)
            .load(fullImageUrl)
            .into(holder.binding.ivProduct)

        // ----------------------------------------------------------------------------------
        // زر الإضافة/الحذف حسب ButtonMode
        when (buttonMode) {
            ButtonMode.ADMIN -> setupButton(holder, "- Delete", R.color.Del_color)
            ButtonMode.CLIENT_HOME -> setupButton(holder, "+ Add", R.color.Suc_color)
            ButtonMode.CLIENT_CART -> setupButton(holder, "- Delete", R.color.Del_color)
            ButtonMode.ADMIN_MANAGE -> setupButton(holder, "administration", R.color.Gray_color, Color.BLACK)
            ButtonMode.NONE -> holder.binding.btnAddProduct.visibility = View.GONE
        }

        // أزرار الكمية فقط في Cart
        if (buttonMode == ButtonMode.CLIENT_CART) {
            holder.binding.btnIncrementce.visibility = View.VISIBLE
            holder.binding.btnDecrements.visibility = View.VISIBLE
            holder.binding.tvQuantity.visibility = View.VISIBLE
            holder.binding.tvQuantity.text = product.quantity.toString()

            holder.binding.btnIncrementce.setOnClickListener {
                product.quantity += 1
                holder.binding.tvQuantity.text = product.quantity.toString()
                onQuantityChanged?.invoke(product)
            }

            holder.binding.btnDecrements.setOnClickListener {
                if (product.quantity > 1) {
                    product.quantity -= 1
                    holder.binding.tvQuantity.text = product.quantity.toString()
                    onQuantityChanged?.invoke(product)
                }
            }

            // زر الحذف في Cart
            holder.binding.btnAddProduct.setOnClickListener {
                product.quantity = 0
                onButtonClick(product)
            }
        } else {
            holder.binding.btnIncrementce.visibility = View.GONE
            holder.binding.btnDecrements.visibility = View.GONE
            holder.binding.tvQuantity.visibility = View.GONE

            if (buttonMode != ButtonMode.NONE) {
                holder.binding.btnAddProduct.setOnClickListener { onButtonClick(product) }
            }
        }

        // الضغط على العنصر
        holder.itemView.setOnClickListener { onItemClick?.invoke(product) }

        // ----------------------------------------------------------------------------------
        // التعامل مع الموقع
        val navigateToLocation: (View) -> Unit = {
            val bundle = bundleOf("lat" to product.latitude, "lng" to product.longitude)
            it.findNavController().navigate(R.id.selectLocationFragment, bundle)
        }
        holder.binding.tvViewLocation.setOnClickListener(navigateToLocation)
        holder.binding.iconLocation.setOnClickListener(navigateToLocation)
    }

    private fun setupButton(holder: ViewHolder, text: String, colorRes: Int, textColor: Int = Color.WHITE) {
        holder.binding.btnAddProduct.text = text
        holder.binding.btnAddProduct.setTextColor(textColor)
        holder.binding.btnAddProduct.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
        holder.binding.btnAddProduct.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int = productList.size

    class ViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)
}
