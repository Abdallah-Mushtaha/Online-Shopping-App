package com.testing.finalprojects.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.onlineshop.adapter.ButtonMode
import com.example.onlineshop.adapter.ProductAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.testing.finalprojects.Api.Api
import com.testing.finalprojects.Model.Category
import com.testing.finalprojects.Model.Product
import com.testing.finalprojects.R
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import java.io.InputStream
import java.util.concurrent.TimeUnit


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AdminShowProductes.newInstance] factory method to
 * create an instance of this fragment.
 */
class AdminShowProductes : Fragment() {
/*
AdminShowProductes للأدمن عشان
يجيب كل المنتجات من الـ API ويعرضها في RecyclerView.
يقدر يحذف منتج.
يقدر يفتح Dialog ويعدل بيانات المنتج (اسم، سعر، وصف، تصنيف، صورة، ومكان).
عملية التعديل (Update) معمولة باستخدام OkHttp Multipart لأنه فيها صورة لازم تنبعت كملف.

ملاحظة
في شاشة Add

كنت مستخدم VolleyMultipartRequest (إنا عامل كلاس داخلي يرّكب الـ multipart بنفسي).
يعني إنا  بتكتب الـ boundaries وبحول البايتات وتضيفهم

اما

هنا بدل ما نعيد كتابة كل شغل الـ multipart، جبت مكتبة جاهزة OkHttp.
إنا بس بقوللها
هاي قيم نصية (FormDataPart).
وهاي ملف (FormDataPart مع RequestBody من نوع image).
وهي لحالها بتبني الـ MultipartBody،
 */

    private val products = mutableListOf<Product>()
    private val categories = mutableListOf<Category>()
    private lateinit var productAdapter: ProductAdapter

    private var selectedImageUri: Uri? = null
    private var currentDialog: Dialog? = null
    private var productToEdit: Product? = null
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var selectedLocationName: String? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 3001
        private const val PRODUCTS_URL = Api.PRODUCTS_URL
        private const val CATEGORIES_URL = Api.CATEGORIES_URL
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_admin_show_productes, container, false)

        val recyclerProducts = view.findViewById<RecyclerView>(R.id.recyclerProducts)
        recyclerProducts.layoutManager = LinearLayoutManager(requireContext())

        productAdapter = ProductAdapter(
            requireContext(),
            products,
            onButtonClick = { product -> deleteProductApi(product) },
            buttonMode = ButtonMode.ADMIN
        )

        productAdapter.onItemClick = { product ->
            productToEdit = product
            showEditProductDialog(product)
        }

        recyclerProducts.adapter = productAdapter

        loadCategoriesFromApi()
        loadProductsFromApi()

        parentFragmentManager.setFragmentResultListener("locationRequest", viewLifecycleOwner) { _, bundle ->
            selectedLat = bundle.getDouble("lat")
            selectedLng = bundle.getDouble("lng")
            selectedLocationName = bundle.getString("locationName") ?: "لم يتم اختيار الموقع"

            productToEdit?.let {
                it.latitude = selectedLat ?: it.latitude
                it.longitude = selectedLng ?: it.longitude
                it.locationName = selectedLocationName ?: it.locationName
                showEditProductDialog(it)
            }
        }

        return view
    }

    // ------------------- Load Categories -------------------
    private fun loadCategoriesFromApi() {
        val request = JsonObjectRequest(Request.Method.GET, CATEGORIES_URL, null,
            { response ->
                val dataArray = response.optJSONArray("data")
                if (dataArray != null) {
                    val type = object : TypeToken<List<Category>>() {}.type
                    val categoryList: List<Category> = Gson().fromJson(dataArray.toString(), type)
                    categories.clear()
                    categories.addAll(categoryList)
                }
            },
            { error ->
                Toast.makeText(requireContext(), "فشل تحميل التصنيفات: ${error.message}", Toast.LENGTH_SHORT).show()
            })
        Volley.newRequestQueue(requireContext()).add(request)
    }

    // ------------------- Load Products -------------------
    private fun loadProductsFromApi(categoryId: Int? = null) {
        val url = if (categoryId != null) "$PRODUCTS_URL/GetAllByCategory/$categoryId" else PRODUCTS_URL
        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val dataArray = response.optJSONArray("data")
                if (dataArray != null) {
                    val type = object : TypeToken<List<Product>>() {}.type
                    val productList: List<Product> = Gson().fromJson(dataArray.toString(), type)
                    products.clear()
                    products.addAll(productList)
                    productAdapter.updateList(products)
                }
            },
            { error ->
                Toast.makeText(requireContext(), "فشل تحميل المنتجات: ${error.message}", Toast.LENGTH_SHORT).show()
            })
        Volley.newRequestQueue(requireContext()).add(request)
    }

    // ------------------- Delete Product -------------------
    private fun deleteProductApi(product: Product) {
        val productId = product.id.toIntOrNull() ?: return
        val url = "$PRODUCTS_URL/$productId"

        val request = object : StringRequest(Method.DELETE, url,
            { _ ->
                Toast.makeText(requireContext(), "تم حذف المنتج", Toast.LENGTH_SHORT).show()
                products.remove(product)
                productAdapter.updateList(products)
            },
            { error ->
                Toast.makeText(requireContext(), "فشل الحذف: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {}
        Volley.newRequestQueue(requireContext()).add(request)
    }

    // ------------------- Edit Product Dialog -------------------
    private fun showEditProductDialog(product: Product) {
        val dialog = Dialog(requireContext())
        currentDialog = dialog
        dialog.setContentView(R.layout.dialog_edit_product_full)
        dialog.setCancelable(true)

        val etName = dialog.findViewById<EditText>(R.id.etProductNameDialog)
        val etPrice = dialog.findViewById<EditText>(R.id.etProductPriceDialog)
        val etDescription = dialog.findViewById<EditText>(R.id.etProductDescriptionDialog)
        val btnSelectLocation = dialog.findViewById<Button>(R.id.btnSelectLocation)
        val btnSelectImage = dialog.findViewById<Button>(R.id.btnSelectImage)
        val btnSave = dialog.findViewById<Button>(R.id.btnSaveDialog)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelDialog)
        val imgPreview = dialog.findViewById<ImageView>(R.id.imgProductPreview)
        val spinnerCategory = dialog.findViewById<Spinner>(R.id.spinnerCategoryDialog)

        etName.setText(product.name ?: "")
        etPrice.setText(product.price.toString())
        etDescription.setText(product.description ?: "")
        Glide.with(this).load(product.imageUrl ?: "").into(imgPreview)

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories.map { it.name }
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        val currentIndex = categories.indexOfFirst { it.name == product.category }
        if (currentIndex >= 0) spinnerCategory.setSelection(currentIndex)

        selectedLat = product.latitude
        selectedLng = product.longitude
        selectedLocationName = product.locationName
        selectedImageUri = null

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnSelectLocation.setOnClickListener {
            dialog.dismiss()
            val bundle = Bundle().apply {
                putDouble("lat", selectedLat ?: product.latitude)
                putDouble("lng", selectedLng ?: product.longitude)
            }
            findNavController().navigate(R.id.selectLocationFragment, bundle)
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newPrice = etPrice.text.toString().toDoubleOrNull()
            val newDescription = etDescription.text.toString().trim()
            val newCategory = spinnerCategory.selectedItem?.toString() ?: product.category

            if (newName.isEmpty() || newPrice == null) {
                Toast.makeText(requireContext(), "أدخل اسم وسعر صالح", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categoryId = categories.firstOrNull { it.name == newCategory }?.id ?: 0
            val productId = product.id.toIntOrNull() ?: return@setOnClickListener

            updateProductApiMultipart(
                productId,
                newName,
                newDescription,
                newPrice,
                categoryId,
                selectedLat ?: product.latitude,
                selectedLng ?: product.longitude,
                selectedImageUri
            )

            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            currentDialog?.findViewById<ImageView>(R.id.imgProductPreview)?.setImageURI(selectedImageUri)
        }
    }

    // ------------------- Update Product Multipart via OkHttp -------------------
    private fun updateProductApiMultipart(
        productId: Int,
        name: String,
        description: String,
        price: Double,
        categoryId: Long,
        latitude: Double,
        longitude: Double,
        imageUri: Uri?
    ) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val url = "$PRODUCTS_URL/$productId"

        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        builder.addFormDataPart("Id", productId.toString())
        builder.addFormDataPart("Name", name)
        builder.addFormDataPart("Description", description)
        builder.addFormDataPart("Price", price.toString())
        builder.addFormDataPart("CategoryId", categoryId.toString())
        builder.addFormDataPart("Latitude", latitude.toString())
        builder.addFormDataPart("Longitude", longitude.toString())

        imageUri?.let {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(it)
            val fileBytes = inputStream?.readBytes()
            inputStream?.close()
            if (fileBytes != null) {
                builder.addFormDataPart(
                    "Image",
                    "product_${System.currentTimeMillis()}.jpg",
                    RequestBody.create("image/*".toMediaTypeOrNull(), fileBytes)
                )
            }
        }

        val requestBody = builder.build()
        val request = okhttp3.Request.Builder()
            .url(url)
            .put(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "فشل التحديث: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                activity?.runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "تم تحديث المنتج بنجاح", Toast.LENGTH_SHORT).show()
                        loadProductsFromApi()
                    } else {
                        Toast.makeText(requireContext(), "فشل التحديث: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}