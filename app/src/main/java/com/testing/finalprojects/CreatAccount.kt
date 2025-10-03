package com.testing.finalprojects

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.onlineshop.LoginActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.testing.finalprojects.Api.Api
import com.testing.finalprojects.Model.User
import com.testing.finalprojects.databinding.ActivityCreatAccountBinding
import org.json.JSONObject
import java.nio.charset.Charset

class CreatAccount : AppCompatActivity() {
    /*
    أنا عامل صفحة اسمها CreatAccount عشان المستخدم يقدر ينشئ حساب جديد.
أول ما يفتح الصفحة بربط الواجهة باستخدام ViewBinding.

لما أضغط على زر إنشاء حساب:
بأخد الإيميل، الباسوورد، تأكيد الباسوورد، الاسم الأول، الاسم الأخير، ورقم الجوال.
بتأكد إنه كل الحقول معبّية.
بتأكد كمان إنه الباسوورد وتأكيده متطابقين.
لو وحدة من هدول مش مظبوطة بوقف وبعرض رسالة خطأ بـ Toast.

بعد هيك بستدعي دالة اسمها registerUser.
فيها بجهز JSON فيه بيانات المستخدم (الإيميل، الباسوورد، الاسم… إلخ).
بستخدم مكتبة Volley عشان أرسل الطلب للسيرفر (API) بطريقة POST.

لو السيرفر رجع نجاح:
بعرض رسالة: "Account created successfully!".
بعدين بروح مباشرة على صفحة تسجيل الدخول LoginActivity.
أما لو السيرفر رجع خطأ (زي إيميل مستخدم قبل أو باسوورد مش قوي):
بفك الرسالة من الرد اللي جاي من السيرفر وبعرضها للمستخدم.
ولو ما قدرت أفك الرسالة بعرض رسالة عامة: "Something went wrong".
كمان ضفت Headers في الطلب (Content-Type = application/json) عشان الـ API يفهم إني ببعت بيانات JSON.

     */
    private lateinit var binding: ActivityCreatAccountBinding
    private val apiUrl = Api.register_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnCreateAccount.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
                firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Password and Confirm Password do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(email, password, confirmPassword, firstName, lastName, phone)
        }
    }

    private fun registerUser(
        email: String,
        password: String,
        confirmPassword: String,
        firstName: String,
        lastName: String,
        phone: String
    ) {
        val queue = Volley.newRequestQueue(this)

        // تجهيز البيانات كـ JSON
        val params = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("confirmPassword", confirmPassword)
            put("firstName", firstName)
            put("lastName", lastName)
            put("phone", phone)
        }

        val jsonRequest = object : JsonObjectRequest(
            Request.Method.POST,
            apiUrl,
            params,
            Response.Listener { response ->
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            },
            Response.ErrorListener { error ->
                var errorMsg = "Something went wrong"
                error.networkResponse?.let {
                    try {
                        val res = String(it.data, Charset.forName("UTF-8"))
                        val jsonError = JSONObject(res)
                        errorMsg = jsonError.optString("message", res)
                    } catch (e: Exception) {
                        Log.e("Volley", "Error parsing response: ${e.message}")
                    }
                }
                Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        queue.add(jsonRequest)
    }
}