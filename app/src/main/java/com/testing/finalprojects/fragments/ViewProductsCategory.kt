package com.testing.finalprojects.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.onlineshop.adapter.ButtonMode
import com.example.onlineshop.adapter.ProductAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.testing.finalprojects.Api.Api
import com.testing.finalprojects.Model.Product
import com.testing.finalprojects.Model.User
import com.testing.finalprojects.R
import com.testing.finalprojects.databinding.FragmentViewProductsCategoryBinding
import org.json.JSONObject

class ViewProductsCategory : Fragment() {
    private var _binding: FragmentViewProductsCategoryBinding? = null
    private val binding get() = _binding!!

    private var products: List<Product> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewProductsCategoryBinding.inflate(inflater, container, false)

        arguments?.getString("productsJson")?.let {
            val type = object : TypeToken<List<Product>>() {}.type
            products = Gson().fromJson(it, type)
        }

        setupRecyclerView()

        binding.fabBack.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        binding.recyclerProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerProducts.adapter = ProductAdapter(
            requireContext(),
            products,
            onButtonClick = { product -> addProductToCart(product) },
            buttonMode = ButtonMode.CLIENT_HOME
        )
    }

    private fun getCurrentUserId(): String {
        val sharedPref = requireActivity().getSharedPreferences("current_user_pref", Context.MODE_PRIVATE)
        val userJson = sharedPref.getString("current_user", null)
        return if (!userJson.isNullOrEmpty()) {
            val user = Gson().fromJson(userJson, User::class.java)
            user.id
        } else {
            "guest"
        }
    }

    private fun getCartPrefFile(): String {
        return "cart_pref_${getCurrentUserId()}"
    }

    private fun addProductToCart(product: Product) {
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


    private fun getToken(): String? {
        val sharedPref = requireActivity().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("token", null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
