package com.testing.finalprojects.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.testing.finalprojects.Api.Api
import com.testing.finalprojects.Model.Category
import com.testing.finalprojects.Model.Product
import com.testing.finalprojects.R
import com.testing.finalprojects.databinding.FragmentAddProductBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class addProductFragment : Fragment() {
    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!

    private val categories = mutableListOf<Category>()
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    private var selectedImageUri: Uri? = null
    private var selectedLat: Double? = 30.0444
    private var selectedLng: Double? = 31.2357
    private var selectedLocationName: String? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
        private const val CATEGORIES_URL = Api.CATEGORIES_URL
        private const val PRODUCTS_URL = Api.PRODUCTS_URL
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)

        loadCategoriesFromApi()

        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        binding.btnSelectLocation.setOnClickListener {
            val bundle = Bundle().apply {
                selectedLat?.let { putDouble("lat", it) }
                selectedLng?.let { putDouble("lng", it) }
            }
            findNavController().navigate(R.id.action_addProductFragment_to_selectLocationFragment, bundle)
        }

        parentFragmentManager.setFragmentResultListener("locationRequest", viewLifecycleOwner) { _, bundle ->
            selectedLat = bundle.getDouble("lat")
            selectedLng = bundle.getDouble("lng")
            selectedLocationName = bundle.getString("locationName") ?: "لم يتم اختيار الموقع"
            binding.btnSelectLocation.text = selectedLocationName
        }

        binding.btnAddProduct.setOnClickListener {
            val name = binding.etProductName.text.toString().trim()
            val description = binding.etProductDescription.text.toString().trim()
            val price = binding.etProductPrice.text.toString().toDoubleOrNull() ?: 0.0
            val categoryName = binding.spinnerProductCategory.selectedItem?.toString() ?: ""
            val categoryId = categories.firstOrNull { it.name == categoryName }?.id?.toInt() ?: 0

            val imageInput = binding.etImageUrl.text.toString().trim() // حقل رابط الصورة

            if (name.isEmpty() || description.isEmpty() || (selectedImageUri == null && imageInput.isEmpty())) {
                Toast.makeText(requireContext(), "أدخل جميع البيانات والصورة أو الرابط", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (imageInput.isNotEmpty() && imageInput.startsWith("http")) {
                // لو المستخدم دخل رابط صورة
                addProductViaApiWithUrl(name, description, price, categoryId, selectedLat ?: 0.0, selectedLng ?: 0.0, imageInput)
            } else if (selectedImageUri != null) {
                val imageFile = File(saveImageFromUri(selectedImageUri!!))
                addProductViaApi(name, description, price, categoryId, selectedLat ?: 0.0, selectedLng ?: 0.0, imageFile)
            }
        }

        return binding.root
    }

    private fun loadCategoriesFromApi() {
        val request = com.android.volley.toolbox.JsonObjectRequest(
            Request.Method.GET, CATEGORIES_URL, null,
            { response ->
                val dataArray = response.optJSONArray("data")
                if (dataArray != null) {
                    val type = object : TypeToken<List<Category>>() {}.type
                    val categoryList: List<Category> = Gson().fromJson(dataArray.toString(), type)
                    categories.clear()
                    categories.addAll(categoryList)

                    spinnerAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        categories.map { it.name }
                    )
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerProductCategory.adapter = spinnerAdapter
                }
            },
            { error ->
                Toast.makeText(requireContext(), "فشل تحميل التصنيفات: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            Glide.with(this).load(selectedImageUri).into(binding.imgProductPreview)
        }
    }

    private fun saveImageFromUri(uri: Uri): String {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = "local_img_${System.currentTimeMillis()}.png"
            val file = File(requireContext().filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /** Volley Multipart عشان ارسل الصوره عبر الفولي ملتي بارت فورم

    عملية رفع الصورة معمولة عن طريق Volley Multipart Request لأن الصورة بدها تنبعت كـ ملف مش مجرد نص
    لما المستخدم يضغط على زر (اختيار صورة)  بيفتح له الـ Gallery
    بعد ما يختار، بتوصل النتيجة   onActivityResult
    بما انت النتيجه هتكون على شكل URi بترجعلي
    saveImageFromUri فهتاخد الصورة ال تم اختيارها وبتنسخها داخل فولدر البرنامج كملف
    .png وترجعلك المسار تبعه

    الان ارسال الصوره للسيرفر
    عامل كلاس داخلي اسمه VolleyMultipartRequest، هذا بيخليني ابعت بيانات مع Multipart Form Data (نفس الفورم تبع الويب لما ترفع صور).
    فيه getParams() بيرجع النصوص العادية (الاسم، الوصف، السعر، إحداثيات الموقع...).
    وفيه getByteData()  بيرجع الصور أو الملفات. وهنا بتحول الصورة لبايتس

    الـ DataPartهو زي كيس صغير بيجمعلك كل المعلومات اللازمة عن أي ملف بدك ترفعه.
    fileName اسم الملف اللي بدك تبعته للسيرفر
    (مثلاً: myimage.png).
    data محتوى الملف نفسه على شكلBytes
    (لأنه لما بدنا نرفع صورة أو أي ملف عبر الشبكة، لازم يتحول لسلسلة أصفار وواحد)"بايت".
    mimeType
    هو نوع الملف (مثلاً: "image/png" أو "image/jpeg" أو "application/pdf").
    getBody()  هذا بيجهز البايتات كلها
    بضيف النصوص (Name, Description, Price, ...).
    بضيف الصورة نفسها (Image).
    وبسكر الـ boundary زي ما لازم بصيغة الـ multipart.
    بعدين
    Volley.newRequestQueue(requireContext()).add(multipartRequest)
    وهنا بنرسل الصورة مع باقي البيانات للسيرفر.
     **/

    abstract class VolleyMultipartRequest(
        method: Int,
        url: String,
        private val listener: Response.Listener<NetworkResponse>,
        errorListener: Response.ErrorListener
    ) : Request<NetworkResponse>(method, url, errorListener) {

        private val boundary = "apiclient-${System.currentTimeMillis()}"

        override fun getBodyContentType(): String = "multipart/form-data;boundary=$boundary"

        override fun getBody(): ByteArray {
            val bos = ByteArrayOutputStream()
            try {
                val params = params
                params?.forEach { (key, value) ->
                    bos.write(("--$boundary\r\n").toByteArray())
                    bos.write("Content-Disposition: form-data; name=\"$key\"\r\n\r\n".toByteArray())
                    bos.write("$value\r\n".toByteArray())
                }

                val byteData = getByteData()
                byteData?.forEach { (key, data) ->
                    bos.write(("--$boundary\r\n").toByteArray())
                    bos.write("Content-Disposition: form-data; name=\"$key\"; filename=\"${data.fileName}\"\r\n".toByteArray())
                    bos.write("Content-Type: ${data.mimeType}\r\n\r\n".toByteArray())
                    bos.write(data.data)
                    bos.write("\r\n".toByteArray())
                }

                bos.write(("--$boundary--\r\n").toByteArray())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bos.toByteArray()
        }

        override fun parseNetworkResponse(response: NetworkResponse?): Response<NetworkResponse> {
            return Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
        }

        override fun deliverResponse(response: NetworkResponse?) {
            listener.onResponse(response!!)
        }

        open fun getByteData(): Map<String, DataPart>? = null

        data class DataPart(val fileName: String, val data: ByteArray, val mimeType: String)
    }

    private fun addProductViaApi(
        name: String,
        description: String,
        price: Double,
        categoryId: Int,
        latitude: Double,
        longitude: Double,
        imageFile: File
    ) {
        val multipartRequest = object : VolleyMultipartRequest(
            Method.POST, PRODUCTS_URL,
            Response.Listener { _ ->
                Toast.makeText(requireContext(), "تم إضافة المنتج بنجاح", Toast.LENGTH_SHORT).show()
                clearForm()
            },
            Response.ErrorListener { error ->
                Toast.makeText(requireContext(), "فشل إضافة المنتج: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf(
                    "Name" to name,
                    "Description" to description,
                    "Price" to price.toString(),
                    "CategoryId" to categoryId.toString(),
                    "Latitude" to latitude.toString(),
                    "Longitude" to longitude.toString()
                )
            }

            override fun getByteData(): Map<String, DataPart>? {
                val bytes = imageFile.readBytes()
                return mapOf("Image" to DataPart(imageFile.name, bytes, "image/png"))
            }
        }

        Volley.newRequestQueue(requireContext()).add(multipartRequest)
    }

    private fun addProductViaApiWithUrl(
        name: String,
        description: String,
        price: Double,
        categoryId: Int,
        latitude: Double,
        longitude: Double,
        imageUrl: String
    ) {
        val multipartRequest = object : VolleyMultipartRequest(
            Method.POST, PRODUCTS_URL,
            Response.Listener { _ ->
                Toast.makeText(requireContext(), "تم إضافة المنتج بنجاح", Toast.LENGTH_SHORT).show()
                clearForm()
            },
            Response.ErrorListener { error ->
                Toast.makeText(requireContext(), "فشل إضافة المنتج: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf(
                    "Name" to name,
                    "Description" to description,
                    "Price" to price.toString(),
                    "CategoryId" to categoryId.toString(),
                    "Latitude" to latitude.toString(),
                    "Longitude" to longitude.toString(),
                    "ImageUrl" to imageUrl // إرسال الرابط بدل الملف
                )
            }

            override fun getByteData(): Map<String, DataPart>? {
                return null // لا يوجد ملف فعلي
            }
        }

        Volley.newRequestQueue(requireContext()).add(multipartRequest)
    }

    private fun clearForm() {
        binding.etProductName.text?.clear()
        binding.etProductPrice.text?.clear()
        binding.etProductDescription.text?.clear()
        binding.imgProductPreview.setImageResource(0)
        selectedImageUri = null
        selectedLat = null
        selectedLng = null
        selectedLocationName = null
        binding.btnSelectLocation.text = getString(R.string.select_location)
        binding.etImageUrl.text?.clear() // مسح حقل الرابط
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
