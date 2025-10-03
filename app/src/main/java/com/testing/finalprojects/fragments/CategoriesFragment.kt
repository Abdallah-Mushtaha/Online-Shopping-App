package com.testing.finalprojects.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onlineshop.adapter.CategoryAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.testing.finalprojects.Model.Category
import com.testing.finalprojects.Model.Product
import com.testing.finalprojects.R
import com.testing.finalprojects.databinding.FragmentCategoriesBinding
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.testing.finalprojects.Api.Api

class CategoriesFragment : Fragment() {
    /*
    بعرض قائمة التصنيفات (Categories) باستخدام RecyclerView + CategoryAdapter.
لما نختار كاتيجوري يستدعي API ثاني يجيب المنتجات الخاصة بهذا التصنيف.
بعد ما يجيب المنتجات  يحولها إلى JSON String (Gson().toJson(products)) ويحطها في Bundle.
يروح بانتقال (Navigation) لواجهة ثانية اسمها viewProductsCategory ويبعث معاها الـ bundle.
ملحوظه

Bundle
ما بيدعم شغل مع اوبجيكتس
الـ Bundle في أندرويد يقدر يخزن أنواع بيانات بسيطة زي
String
Int
Boolean
Parcelable
لكن ما يقدر يخزن List<Product> مباشرة (إلا لو حولت Product لـ Parcelable، وهذا شغل طويل).

3️⃣ الحل نحويل القائمة لنص (String) باستخدام Gson
هنا يجي دور
Gson().toJson(products)
toJson(products) يحول القائمة كلها من اوبجيكتس
 إلى نص JSON عادي (String).




استخدامت Navigation مع Bundle
بدل ما تفتح Dialog أو تحدث Recycler في نفس الصفحة،  بيروح لشاشة ثانية ويرسل بيانات المنتجات.
val bundle = Bundle().apply {
    putString("productsJson", Gson().toJson(products))
    putInt("categoryId", categoryId)
}
findNavController().navigate(R.id.action_categoriesFragment_to_viewProductsCategory, bundle)

طريقة تمرير البيانات
ما بيخزن المنتجات في SharedPreferences أو يطلبها من جديد في الصفحة الثانية، لأ… بيرسلهم مباشرة كسلسلة JSON.
(وهذا ذكي لأنه يوفر اتصال إضافي بالسيرفر).
CategoryAdapter هون معمول بـ isAdmin = false → يعني واجهة المستخدم العادي (ما فيها زر حذف/تعديل زي واجهة الأدمن).

اشياء تعاملنا معها سابقا
استخدام Volley (JsonObjectRequest) للاتصال بالـ API.

تحويل JSONArray إلى List من الاوبجيكتس (باستخدام Gson + TypeToken).
التعامل مع RecyclerView + Adapter.
التعامل مع الأخطاء وإظهار Toast.

     */

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    private val categories = mutableListOf<Category>()
    private lateinit var categoryAdapter: CategoryAdapter

    private val categoriesUrl = Api.CATEGORIES_URL
    private val productsBaseUrl = Api.GetAllByCategory_URL

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)

        binding.recyclerCategories.layoutManager = LinearLayoutManager(requireContext())

        categoryAdapter = CategoryAdapter(
            requireContext(),
            categories,
            onDeleteClick = { },
            onItemClick = { category ->
                loadProductsForCategory(category.id.toInt())
            },
            isAdmin = false,
            onEditClick = {}
        )

        binding.recyclerCategories.adapter = categoryAdapter
        loadCategoriesFromApi()

        return binding.root
    }

    private fun loadCategoriesFromApi() {
        val request = JsonObjectRequest(
            Request.Method.GET, categoriesUrl, null,
            { response ->
                categories.clear()
                val dataArray = response.getJSONArray("data")
                val type = object : TypeToken<List<Category>>() {}.type
                val categoryList: List<Category> = Gson().fromJson(dataArray.toString(), type)
                categories.addAll(categoryList)
                categoryAdapter.notifyDataSetChanged()
            },
            { error ->
                Toast.makeText(requireContext(), "فشل تحميل التصنيفات: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun loadProductsForCategory(categoryId: Int) {
        val url = "$productsBaseUrl/$categoryId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val dataArray = response.getJSONArray("data")
                val type = object : TypeToken<List<Product>>() {}.type
                val products: List<Product> = Gson().fromJson(dataArray.toString(), type)

                val bundle = Bundle().apply {
                    putString("productsJson", Gson().toJson(products))
                    putInt("categoryId", categoryId)
                }
                findNavController().navigate(R.id.action_categoriesFragment_to_viewProductsCategory, bundle)
            },
            { error ->
                Toast.makeText(requireContext(), "فشل تحميل المنتجات: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
