package com.testing.finalprojects.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.onlineshop.adapter.ButtonMode
import com.example.onlineshop.adapter.ProductAdapter
import com.testing.finalprojects.Api.Api
import com.testing.finalprojects.Model.Product
import com.testing.finalprojects.databinding.FragmentPurchasedBinding

class PurchasedFragment : Fragment() {
    /*
هذا هو شاشة المشتريات السابقة (Purchased Orders).
لما يفتح المستخدم الصفحة
يجلب الطلبات من API GET_ORDERS_BY_CUSTOMER_URL.
الطلب بيرجع قائمة طلبات، كل طلب يحتوي على items.
كل Item فيه productId, unitPrice, quantity.
ينشئ منتجات مؤقتة (اسمها "جاري التحميل...").
لكل منتج  يستدعي API ثاني Products/{id}  ليجيب تفاصيل المنتج الكاملة (اسم، صورة، وصف).
يدمج البيانات ويرجع يعرضها في RecyclerView.
    -----------------------------------------
    التعامل مع Orders  Items  Products
هنا ما بتجيب المنتجات مباشرة من Products API.
بتجيب أولًا "الطلبات" (Orders) وبعدين تمشي جوه items عشان تطلع المنتجات.

دمج بيانات من مصدرين
الطلب الأول يرجع بيانات ناقصة (ID, السعر, الكمية).
الطلب الثاني يكمل الباقي (اسم, صورة, وصف).
يعني في مرحلتين: تحميل مؤقت  تحديث كامل.

عرض رسالة لو مافي مشتريات
إذا الـ Array فاضي يخفي RecyclerView ويظهر tvNoPurchasedItems.

شرط البحث عن المنتج مع الكمية:
val index = purchasedProducts.indexOfFirst { it.id == orderItem.id && it.quantity == orderItem.quantity }
مش بس يبحث بالـ id.
كمان يتأكد إن الكمية نفسها (لأنه ممكن نفس المنتج يتكرر بكمية مختلفة في أوامر مختلفة).
زر Back (fabBack)
 بيرجع المستخدم للصفحة السابقة عن طريق
فيه Floating Action Button
findNavController().popBackStack()

     */

    private var _binding: FragmentPurchasedBinding? = null
    private val binding get() = _binding!!

    // القائمة التي ستعرض المنتجات المشتراة المدمجة البيانات
    private val purchasedProducts = mutableListOf<Product>()
    private lateinit var purchasedAdapter: ProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPurchasedBinding.inflate(inflater, container, false)

        binding.recyclerPurchased.layoutManager = LinearLayoutManager(requireContext())

        purchasedAdapter = ProductAdapter(
            requireContext(),
            purchasedProducts,
            onButtonClick = { /* do nothing */ },
            buttonMode = ButtonMode.NONE
        )

        binding.recyclerPurchased.adapter = purchasedAdapter

        binding.fabBack.setOnClickListener {
            findNavController().popBackStack()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // يتم تحميل المنتجات عند إنشاء View
        loadPurchasedProducts()
    }

    // دالة للحصول على التوكن من SharedPreferences
    private fun getToken(): String? {
        val sharedPref = requireActivity().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("token", null)
    }

    private fun loadPurchasedProducts() {
        val url = Api.GET_ORDERS_BY_CUSTOMER_URL
        val token = getToken()
        val ctx = context ?: return

        if (token.isNullOrEmpty()) {
            Toast.makeText(ctx, "الرجاء تسجيل الدخول لعرض المشتريات.", Toast.LENGTH_LONG).show()
            binding.recyclerPurchased.visibility = View.GONE
            binding.tvNoPurchasedItems.visibility = View.VISIBLE
            binding.tvNoPurchasedItems.text = "الرجاء تسجيل الدخول"
            return
        }

        val requestQueue = Volley.newRequestQueue(ctx)

        val jsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (!isAdded) returnTransition
                try {
                    val dataArray = response.optJSONArray("data")

                    purchasedProducts.clear()

                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val orderObject = dataArray.getJSONObject(i)
                            // افترضنا أن المفتاح هو "items"
                            val orderItemsArray = orderObject.optJSONArray("items")

                            if (orderItemsArray != null) {
                                for (j in 0 until orderItemsArray.length()) {
                                    val item = orderItemsArray.getJSONObject(j)

                                    val productId = item.optInt("productId")
                                    val price = item.optDouble("unitPrice", 0.0)
                                    val quantity = item.optInt("quantity", 1) // ✅ جلب الكمية

                                    // إنشاء اوبجيكت مؤقت بالبيانات الأساسية
                                    val tempProduct = Product(
                                        id = productId.toString(),
                                        name = "جاري التحميل...",
                                        price = price,
                                        rate = 0f,
                                        categoryId = 0,
                                        quantity = quantity,
                                        image = ""
                                    )
                                    purchasedProducts.add(tempProduct)
                                    // استدعاء جلب التفاصيل ودمجها لكل منتج
                                    fetchProductDetailsAndMerge(tempProduct)
                                }
                            }
                        }
                    }

                    // تحديث مبدئي لعرض حالة أو "لا توجد مشتريات"
                    if (purchasedProducts.isEmpty()) {
                        binding.recyclerPurchased.visibility = View.GONE
                        binding.tvNoPurchasedItems.visibility = View.VISIBLE
                        binding.tvNoPurchasedItems.text = "لا توجد مشتريات سابقة."
                    } else {
                        binding.recyclerPurchased.visibility = View.VISIBLE
                        binding.tvNoPurchasedItems.visibility = View.GONE
                        purchasedAdapter.updateList(purchasedProducts)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(ctx, "خطأ في تحليل بيانات الطلبات: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                if (!isAdded) returnTransition
                error.printStackTrace()
                val statusCode = error.networkResponse?.statusCode
                val errorMessage = if (statusCode == 401) {
                    "غير مصرح: الرجاء التأكد من التوكن وصحة الجلسة."
                } else {
                    "فشل الاتصال بالسيرفر. الكود: ${statusCode ?: "لا يوجد"}"
                }
                Toast.makeText(ctx, errorMessage, Toast.LENGTH_LONG).show()
                binding.tvNoPurchasedItems.text = errorMessage
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                // إرسال التوكن في الهيدر
                token?.let { headers["Authorization"] = "Bearer $it" }
                return headers
            }
        }

        requestQueue.add(jsonObjectRequest)
    }


    private fun fetchProductDetailsAndMerge(orderItem: Product) {
        val url = "${Api.PRODUCTS_URL}/${orderItem.id}"
        val ctx = context ?: return
        val requestQueue = Volley.newRequestQueue(ctx)

        val jsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (!isAdded) returnTransition
                try {
                    val dataObject = response.optJSONObject("data")
                    if (dataObject != null) {

                        // تحليل البيانات المكتملة
                        val name = dataObject.optString("name", "اسم غير متوفر")
                        val description = dataObject.optString("description", "")
                        val imageUrl = dataObject.optString("imageUrl", "")

                        // البحث عن المنتج في قائمة المشتريات لتحديثه
                        val index = purchasedProducts.indexOfFirst { it.id == orderItem.id && it.quantity == orderItem.quantity }

                        if (index != -1) {
                            // إنشاء كائن Product جديد بجميع البيانات
                            val updatedProduct = purchasedProducts[index].copy(
                                name = name,
                                description = description,
                                imageUrl = imageUrl,

                                price = orderItem.price,
                                quantity = orderItem.quantity // الحفاظ على الكمية ال اجت من الطلب
                            )

                            // استبدال الاوبجيكت القديم بالجديد المحدث
                            purchasedProducts[index] = updatedProduct

                            // تحديث Adapter
                            purchasedAdapter.updateList(purchasedProducts)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                if (!isAdded) returnTransition
                error.printStackTrace()
            }
        ) {}

        requestQueue.add(jsonObjectRequest)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}