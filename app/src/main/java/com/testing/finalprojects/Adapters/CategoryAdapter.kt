package com.example.onlineshop.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.testing.finalprojects.Model.Category
import com.testing.finalprojects.databinding.ItemCategoryBinding


class CategoryAdapter(
    private val context: Context,
    private var list: MutableList<Category>,
    private val onDeleteClick: (Category) -> Unit,
    private val onEditClick: (Category) -> Unit,
    private val onItemClick: (Category) -> Unit,
    private val isAdmin: Boolean = false
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
//        عملت ليست للكاتيجوري  للتخزينها وعرضتهم
        val category = list[position]
        holder.binding.tvCategoryName.text = category.name

//هنا قلت والله في حال انت ادمن رح يكون الك صلاحيه تحذف هذا الكتيجوري
        holder.binding.btnDeleteCategory.visibility = if (isAdmin) View.VISIBLE else View.GONE
        holder.binding.btnDeleteCategory.setOnClickListener {
            onDeleteClick(category)
        }

//      قلتلو في حال ضغط على العنصر نفسو بدي اعمل تعديل على الكاتيجوري
        holder.binding.root.setOnClickListener {
            if (isAdmin) {
                onEditClick(category)
            } else {
                onItemClick(category)
            }
        }
    }

    override fun getItemCount(): Int = list.size
// عشان احدث الكاتيجوري في حال صار اي تغير عليها زي اني اضفت
    fun updateList(newList: MutableList<Category>) {
        list = newList
        notifyDataSetChanged()
    }
}

