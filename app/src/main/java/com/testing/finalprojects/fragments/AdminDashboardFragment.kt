package com.testing.finalprojects.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.onlineshop.adapter.CategoryAdapter
import com.example.onlineshop.adapter.ProductAdapter
import com.example.onlineshop.adapter.ButtonMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.testing.finalprojects.Api.Api
import com.testing.finalprojects.Model.Category
import com.testing.finalprojects.Model.Product
import com.testing.finalprojects.R
import com.testing.finalprojects.databinding.FragmentAdminDashboardBinding
import org.json.JSONObject

class AdminDashboardFragment : Fragment() {
    /*
    هاي الشاشة اسمها AdminDashboardFragment، يعني لوحة تحكم الأدمن.
الأدمن يقدر يعمل حاجتين أساسيات
يتحكم بالـ تصنيفات (Categories)  يضيف، يعدل، يحذف.
يشوف المنتجات الأكثر شراءً (Most Purchased Products) ويعرضها في RecyclerView أفقي

ك لعاده بحمل الكاتيجوري ل عرضها وبضيفهم وبعدل عليهم من خلال ديلوج
وكمان  بحمل ال اعرض المنتجات ال تم شرائها  من لتطبيق واذا في
 بيانات لمنتج ناقصه بجيبها حسب ال id تبع المنتج هذا

     */

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private val categories = mutableListOf<Category>()
    private lateinit var categoryAdapter: CategoryAdapter

    private val mostPurchasedProducts = mutableListOf<Product>()
    private lateinit var mostPurchasedAdapter: ProductAdapter

    private val baseUrl = Api.CATEGORIES_URL

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupAddCategoryButton()
        setupPurchasedRecycler()

        loadCategories()

        return binding.root
    }


    private fun setupRecyclerView() {
        binding.recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        categoryAdapter = CategoryAdapter(
            requireContext(),
            categories,
            onDeleteClick = { category -> deleteCategory(category.id) },
            onEditClick = { category -> showCustomEditDialog(category) },
            onItemClick = {},
            isAdmin = true
        )
        binding.recyclerCategories.adapter = categoryAdapter
    }


    private fun setupAddCategoryButton() {
        binding.btnAddCategory.setOnClickListener {
            val name = binding.etCategoryName.text.toString()
            if (name.isNotEmpty()) {
                addCategory(name)
                binding.etCategoryName.text.clear()
            }
        }
    }


    private fun loadCategories() {
        val request = JsonObjectRequest(Request.Method.GET, baseUrl, null,
            { response ->
                categories.clear()
                val dataArray = response.getJSONArray("data")
                val type = object : TypeToken<List<Category>>() {}.type
                val categoryList: List<Category> = Gson().fromJson(dataArray.toString(), type)
                categories.addAll(categoryList)
                categoryAdapter.updateList(categories)
            },
            { error ->
                Toast.makeText(requireContext(), "فشل تحميل التصنيفات: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun addCategory(name: String) {
        val jsonBody = JSONObject()
        jsonBody.put("name", name)
        val request = JsonObjectRequest(Request.Method.POST, baseUrl, jsonBody,
            { response ->
                val message = response.optString("message", "تمت الإضافة")
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                loadCategories()
            },
            { error ->
                Toast.makeText(requireContext(), "فشل إضافة التصنيف: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun updateCategory(id: Long, newName: String) {
        val url = "$baseUrl/$id"
        val jsonBody = JSONObject()
        jsonBody.put("id", id)
        jsonBody.put("name", newName)
        val request = JsonObjectRequest(Request.Method.PUT, url, jsonBody,
            { response ->
                val message = response.optString("message", "تم التعديل")
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                loadCategories()
            },
            { error ->
                Toast.makeText(requireContext(), "فشل تعديل التصنيف: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun deleteCategory(id: Long) {
        val url = "$baseUrl/$id"
        val request = StringRequest(Request.Method.DELETE, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val message = json.optString("message", "تم الحذف")
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "تم الحذف", Toast.LENGTH_SHORT).show()
                }
                loadCategories()
            },
            { error ->
                Toast.makeText(requireContext(), "فشل حذف التصنيف: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun showCustomEditDialog(category: Category) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_edit_category)
        dialog.setCancelable(true)

        val etName = dialog.findViewById<EditText>(R.id.etCategoryNameDialog)
        val btnSave = dialog.findViewById<Button>(R.id.btnSaveDialog)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelDialog)

        etName.setText(category.name)

        btnSave.setOnClickListener {
            val newName = etName.text.toString()
            if (newName.isNotEmpty()) {
                updateCategory(category.id, newName)
            }
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /** RecyclerView للمنتجات **/
    private fun setupPurchasedRecycler() {
        binding.recyclerMostPurchased.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        mostPurchasedAdapter = ProductAdapter(
            requireContext(),
            mostPurchasedProducts,
            onButtonClick = {},
            buttonMode = ButtonMode.NONE
        )
        binding.recyclerMostPurchased.adapter = mostPurchasedAdapter
        loadProducts()
    }


    private fun loadProducts() {
        val sharedPref = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""
        val productsUrl = Api.PRODUCTS_URL
        val request = object : JsonObjectRequest(
            Method.GET, productsUrl, null,
            { response ->
                try {
                    mostPurchasedProducts.clear()
                    val dataArray = response.getJSONArray("data")
                    for (i in 0 until dataArray.length()) {
                        val data = dataArray.getJSONObject(i)
                        val product = Product(
                            id = data.getInt("id").toString(),
                            name = data.getString("name"),
                            description = data.getString("description"),
                            imageUrl = data.optString("imageUrl", ""),
                            price = data.getDouble("price"),
                            rate = data.getDouble("rate").toFloat(),
                            latitude = data.getDouble("latitude"),
                            longitude = data.getDouble("longitude"),
                            categoryId = data.getInt("categoryId"),
                            quantity = 5,
                            category = "",
                            image = "",
                            location = "",
                            locationName = ""
                        )
                        mostPurchasedProducts.add(product)
                    }
                    mostPurchasedAdapter.updateList(mostPurchasedProducts)
                    binding.recyclerMostPurchased.visibility =
                        if (mostPurchasedProducts.isEmpty()) View.GONE else View.VISIBLE
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "خطأ في قراءة بيانات المنتجات", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "فشل تحميل المنتجات: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf("Authorization" to "Bearer $token")
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
