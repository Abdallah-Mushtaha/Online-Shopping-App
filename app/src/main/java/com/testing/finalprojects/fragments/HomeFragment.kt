package com.testing.finalprojects.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.onlineshop.adapter.ButtonMode
import com.example.onlineshop.adapter.ProductAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.testing.finalprojects.Api.Api
import com.testing.finalprojects.Model.Product
import com.testing.finalprojects.R
import com.testing.finalprojects.databinding.FragmentHomeBinding
import org.json.JSONObject

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class HomeFragment : Fragment() {
    /*
    SearchView (البحث)
ضفت SearchView عشان المستخدم يقدر يكتب اسم المنتج أو حتى رقم (سعر).
الكود يشيك
إذا المستخدم كتب كلمة  يرسلها كـ name.
إذا كتب رقم  يحاول يحوله لـ price.
يعني البحث يدعم نوعين بالاسم أو بالسعر.
هذا يتم في دالة
private fun filterProducts(query: String) { ... }

استخدامت StringRequest مع POST

بدل ما يرسل JsonObjectRequest، هنا استخدم StringRequest مع جسم (Body) مخصص.

ليش؟ لأن السيرفر بيرجع Array مباشرة (مش Object فيه success/data).

عشان هيك
override fun getBody(): ByteArray {
    return filterBodyString.toByteArray(Charsets.UTF_8)
}

بيخلي الطلب يحمل JSON نصي يرسله للسيرفر.
إضافة منتجات إلى السلة من الصفحة الرئيسية
لما المستخدم يضغط زر (على كل منتج)  ينادي
addToCart(product)
يبني جسم الطلب
{
  "productId": ...,
  "unitPrice": ...,
  "quantity": 1
}
ويرسله مع Header فيه Authorization Bearer Token (إذا كان المستخدم مسجل دخول).

باقي الشغل زي ما عملتو قبل في الفراجمينتس
     */
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val originalProducts = mutableListOf<Product>()
    private lateinit var productAdapter: ProductAdapter

    private val mostOrderedProducts = mutableListOf<Product>()
    private lateinit var mostOrderedAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMostOrderedRecyclerView()
        loadProducts()
        loadMostOrderedProducts()
        setupSearchView()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            requireContext(),
            mutableListOf(),
            onButtonClick = { product -> addToCart(product) },
            buttonMode = ButtonMode.CLIENT_HOME
        )
        binding.recyclerProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerProducts.adapter = productAdapter
    }

    // إعداد RecyclerView للمنتجات الأكثر طلباً
    private fun setupMostOrderedRecyclerView() {
        mostOrderedAdapter = ProductAdapter(
            requireContext(),
            mutableListOf(),
            onButtonClick = { product -> addToCart(product) },
            buttonMode = ButtonMode.CLIENT_HOME,
                    showTotalOrdered = true
        )
        binding.recyclerMostOrderedProducts.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerMostOrderedProducts.adapter = mostOrderedAdapter
    }

    private fun loadProducts() {
        val url = Api.PRODUCTS_URL
        val requestQueue = Volley.newRequestQueue(requireContext())

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val success = response.getBoolean("success")
                    if (success) {
                        val dataArray = response.getJSONArray("data")
                        val gson = Gson()
                        val type = object : TypeToken<MutableList<Product>>() {}.type
                        val productList: MutableList<Product> =
                            gson.fromJson(dataArray.toString(), type)

                        originalProducts.clear()
                        originalProducts.addAll(productList)
                        productAdapter.updateList(originalProducts)
                    } else {
                        Toast.makeText(requireContext(), "فشل في تحميل المنتجات", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "خطأ في قراءة البيانات", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "فشل الاتصال بالسيرفر", Toast.LENGTH_SHORT).show()
            }
        )

        requestQueue.add(jsonObjectRequest)
    }

    // جلب المنتجات الأكثر طلباً من السيرفر
    private fun loadMostOrderedProducts() {
        val url = "https://9a9f9a863fb9.ngrok-free.app/api/Products/GetMostOrderedProducts"
        val requestQueue = Volley.newRequestQueue(requireContext())

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val success = response.getBoolean("success")
                    if (success) {
                        val dataArray = response.getJSONArray("data")
                        val gson = Gson()
                        val type = object : TypeToken<MutableList<Product>>() {}.type
                        val productList: MutableList<Product> = gson.fromJson(dataArray.toString(), type)

                        mostOrderedProducts.clear()
                        mostOrderedProducts.addAll(productList)
                        mostOrderedAdapter.updateList(mostOrderedProducts)
                    } else {
                        Toast.makeText(requireContext(), "فشل في تحميل أكثر المنتجات طلباً", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "خطأ في قراءة البيانات", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "فشل الاتصال بالسيرفر", Toast.LENGTH_SHORT).show()
            }
        )

        requestQueue.add(jsonObjectRequest)
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterProducts(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    productAdapter.updateList(originalProducts)
                }
                return true
            }
        })
    }

    // دالة لاسترجاع التوكن من الـ SharedPreferences
    private fun getToken(): String? {
        val sharedPref = requireActivity().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("token", null)
    }

    // دالة لتصفية المنتجات حسب الاسم أو السعر
    private fun filterProducts(query: String) {
        val url = Api.PRODUCTS_FILTER_URL
        val requestQueue = Volley.newRequestQueue(requireContext())

        val priceValue = query.toDoubleOrNull()
        val filterBodyString: String

        // طريقة البحث مره هتكون من خلال  السعر ومره من خلال الاسم
        if (priceValue != null && priceValue > 0) {
            filterBodyString = JSONObject().apply {
                put("name", "")
                put("price", priceValue)
                put("rate", 0)
            }.toString()
        } else {
            filterBodyString = JSONObject().apply {
                put("name", query)
                put("price", 0)
                put("rate", 0)
            }.toString()
        }

        val stringRequest = object : com.android.volley.toolbox.StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val type = object : TypeToken<MutableList<Product>>() {}.type
                    val filteredList: MutableList<Product> = Gson().fromJson(response, type)
                    productAdapter.updateList(filteredList)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "خطأ أثناء قراءة البحث: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "فشل الاتصال بالسيرفر", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }

            override fun getBody(): ByteArray {
                return filterBodyString.toByteArray(Charsets.UTF_8)
            }
        }

        requestQueue.add(stringRequest)
    }

    // إضافة المنتج للسلة
    private fun addToCart(product: Product) {
        val url = Api.ADD_TO_CART_URL
        val requestQueue = Volley.newRequestQueue(requireContext())

        val body = JSONObject().apply {
            put("productId", product.id)
            put("unitPrice", product.price)
            put("quantity", 1)
        }

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, body,
            { response ->
                Toast.makeText(requireContext(), "${product.name} تمت إضافته إلى السلة", Toast.LENGTH_SHORT).show()
            },
            { error ->
                error.printStackTrace()

                val errorMessage = if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    "فشل الإضافة: الرجاء تسجيل الدخول."
                } else {
                    "فشل إضافة المنتج للسلة: ${error.message ?: "خطأ في الاتصال"}"
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                val token = getToken()
                if (!token.isNullOrEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                }
                return headers
            }
        }

        requestQueue.add(jsonObjectRequest)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
