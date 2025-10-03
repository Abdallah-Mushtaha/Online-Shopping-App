package com.testing.finalprojects.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.onlineshop.adapter.ButtonMode
import com.example.onlineshop.adapter.ProductAdapter
import com.testing.finalprojects.Api.Api
import com.testing.finalprojects.Model.Product
import com.testing.finalprojects.databinding.FragmentCartBinding
import org.json.JSONObject

class CartFragment : Fragment() {
    /*
         شاشة السلة (Cart) للزبون
بيعرض المنتجات اللي موجودة بالسلة
  يخلي المستخدم يعدّل الكمية
يحذف منتج
 يعمل Checkout (شراء نهائي)
 ومعتمد بدا لو في داتا ناقصه لمنتج معين جيبهم من خلال ال id تبع هذا لمنتج
     */
    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    // القائمة النهائية التي سيتم عرضها، تحتوي على بيانات السلة مدمجة مع تفاصيل المنتج
    private val cartProducts = mutableListOf<Product>()

    private lateinit var cartAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)

        binding.recyclerCart.layoutManager = LinearLayoutManager(requireContext())

        cartAdapter = ProductAdapter(
            requireContext(),
            cartProducts,
            onButtonClick = { product -> deleteCartItem(product) },
            buttonMode = ButtonMode.CLIENT_CART
        )

        cartAdapter.onQuantityChanged = { addOrUpdateCartItem(it) }

        binding.recyclerCart.adapter = cartAdapter

        loadCart()

        binding.btnCheckout.setOnClickListener {
            if (cartProducts.isNotEmpty()) checkout()
            else Toast.makeText(requireContext(), "السلة فارغة", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }
    //  إضافة loadCart()
    //  هنا لضمان التحديث عند العودة للـ Fragment
    override fun onResume() {
        super.onResume()
        loadCart()
    }


    private fun getToken(): String? {
        val sharedPref = activity?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPref?.getString("token", null)
    }


    private fun loadCart() {
        val url = Api.GET_CART_URL
        val ctx = context ?: return
        val requestQueue = Volley.newRequestQueue(ctx)

        // ** تصحيح الأخطاء الإملائية والـ return **
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.GET, url, null,
            { response ->
                if (!isAdded) returnTransition
                try {
                    val dataObject = response.optJSONObject("data")
                    val itemsArray = dataObject?.optJSONArray("items")

                    cartProducts.clear()
                    if (itemsArray != null) {
                        for (i in 0 until itemsArray.length()) {
                            val item = itemsArray.getJSONObject(i)

                            val productId = item.optInt("productId")
                            val price = item.optDouble("unitPrice", 0.0)
                            val quantity = item.optInt("quantity", 1)

                            // بحتفظ ببيانات السلة مؤقتاً
                            val cartItem = Product(
                                id = productId.toString(),
                                name = "جاري التحميل...",
                                price = price,
                                rate = 0f,
                                categoryId = 0,
                                quantity = quantity,
                                image = ""
                            )
                            cartProducts.add(cartItem)

                            fetchProductDetailsAndMerge(cartItem)
                        }
                    } else {
                        // إذالسلة فاضية، اعمل تحديث
                        cartAdapter.updateList(cartProducts)
                    }



                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "خطأ في تحليل بيانات السلة: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                if (!isAdded) returnTransition
                error.printStackTrace()
                val errorMessage = if (error.networkResponse?.statusCode == 401) {
                    "الرجاء تسجيل الدخول لعرض السلة"
                } else {
                    "فشل الاتصال بالسيرفر. الكود: ${error.networkResponse?.statusCode ?: "لا يوجد"}"
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                getToken()?.let { headers["Authorization"] = "Bearer $it" }
                return headers
            }
        }

        requestQueue.add(jsonObjectRequest)
    }


    private fun fetchProductDetailsAndMerge(cartItem: Product) {

        val url = "${Api.PRODUCTS_URL}/${cartItem.id}"
        val ctx = context ?: return
        val requestQueue = Volley.newRequestQueue(ctx)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (!isAdded) return@JsonObjectRequest
                try {
                    val dataObject = response.optJSONObject("data")
                    /*
                    response.optJSONObject("data")
                    دور جوه response على مفتاح اسمه "data".

لو لقيت القيمة وكانت كائن JSON  رجعها كـ JSONObject.

لو المفتاح مش موجود أو القيمة مش كائن  رجع null (بدون ما يرمي Exception).

getJSONObject("key")  لو المفتاح مش موجود يطلع خطأ (Exception).

optJSONObject("key")  يرجع null بأمان.
                     */
                    if (dataObject != null) {


                        val name = dataObject.optString("name", "اسم غير متوفر")
                        val description = dataObject.optString("description", "")
                        val imageUrl = dataObject.optString("imageUrl", "")

                        // دور عن مكان المنتج في قائمة السلة لتحديثه
                        val index = cartProducts.indexOfFirst { it.id == cartItem.id }

                        if (index != -1) {
                            //  Product
                            //  جديد بجميع البيانات
                            val updatedProduct = cartProducts[index].copy(
                                name = name,
                                description = description,
                                imageUrl = imageUrl
                            )

                            // استبدال الاوبجيكت القديم بالجديد المحدث
                            cartProducts[index] = updatedProduct

                            // تحديث Adapter
                            cartAdapter.updateList(cartProducts)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()

                }
            },
            { error ->
                if (!isAdded) return@JsonObjectRequest
                error.printStackTrace()
                /*

isAdded خاصية موجودة في Fragment.
معناها: هل هذا الـ Fragment ما زال متوصل (Attached) بالنشاط (Activity)
أحيانًا الـ Fragment يكون اتدمر أو اتنقل قبل ما يجيك رد من الـ API.
مثال:
 فتحت شاشة "CartFragment".
بعدها طلعت منها بسرعة للشاشة اللي بعدها.
بنفس الوقت، طلب الشبكة (Volley Request) لسه شغال.
فجأة يوصلك الرد، لكن الـ Fragment خلاص مش موجود  لو حاولت تستخدم requireContext() أو binding بيطيح التطبيق.
 "لو الـ Fragment مش مضاف/موجود حاليًا → اطلع من الكود تبع الـ Callback تبع Volley، وما تعمل أي تحديث عالشاشة.
عشان تمنع الكراش لما المستخدم يترك الشاشة قبل ما يوصل الرد من السيرفر.

                 */

            }
        )

        requestQueue.add(jsonObjectRequest)
    }

    private fun addOrUpdateCartItem(product: Product) {
        val url = Api.ADD_TO_CART_URL
        val ctx = context ?: return
        val requestQueue = Volley.newRequestQueue(ctx)

        val body = JSONObject().apply {
            put("ProductId", product.id.toInt())
            put("UnitPrice", product.price)
            put("Quantity", product.quantity)
        }

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, body,
            { /* تحديث ناجح */ },
            { error ->
                error.printStackTrace()
                Toast.makeText(ctx, "فشل تحديث الكمية", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                getToken()?.let { headers["Authorization"] = "Bearer $it" }
                return headers
            }
        }

        requestQueue.add(jsonObjectRequest)
    }

    private fun deleteCartItem(product: Product) {
        val url = "${Api.REMOVE_FROM_CART_URL}/${product.id}"
        val ctx = context ?: return
        val requestQueue = Volley.newRequestQueue(ctx)

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.DELETE, url, null,
            {
                if (!isAdded) returnTransition
                cartProducts.remove(product)
                cartAdapter.updateList(cartProducts)
                Toast.makeText(requireContext(), "تم حذف المنتج من السلة", Toast.LENGTH_SHORT).show()
            },
            { error ->
                error.printStackTrace()
                if (isAdded) Toast.makeText(requireContext(), "فشل حذف المنتج", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                getToken()?.let { headers["Authorization"] = "Bearer $it" }
                return headers
            }
        }

        requestQueue.add(jsonObjectRequest)
    }

    private fun checkout() {
        val url = Api.CHECKOUT_URL
        val ctx = context ?: return
        val requestQueue = Volley.newRequestQueue(ctx)

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, null,
            {
                if (!isAdded) returnTransition // هو Property موجودة في Fragment أصلًا (بتخص الانتقالات بين الـ Fragments)،
/*
isAdded
 زي ما شرحت قبل شوي = بتتأكد إذا الـ
 Fragment
  لسه موجود ومربوط بالنشاط
 (Activity).
المفروض إذا مش موجود توقف الكود وما تكمل معالجة الرد
 */
                Toast.makeText(requireContext(), "تم إتمام عملية الشراء بنجاح!", Toast.LENGTH_SHORT).show()
                cartProducts.clear()
                cartAdapter.updateList(cartProducts)
            },
            { error ->
                error.printStackTrace()
                if (isAdded) Toast.makeText(requireContext(), "فشل إتمام عملية الشراء", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                getToken()?.let { headers["Authorization"] = "Bearer $it" }
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