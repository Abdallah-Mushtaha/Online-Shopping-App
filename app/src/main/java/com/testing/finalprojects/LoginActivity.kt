package com.example.onlineshop

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.testing.finalprojects.Api.Api
import com.testing.finalprojects.CreatAccount
import com.testing.finalprojects.MainActivity
import com.testing.finalprojects.databinding.ActivityLoginBinding
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
/*
عامل صفحة LoginActivity عشان أسجل دخول المستخدم.
ببدأ بربط الواجهة باستخدام ViewBinding.

لما المستخدم يضغط زر تسجيل الدخول:
بأخد الإيميل (username) والباسوورد.
بتأكد إنهم مش فاضيين.
لو تمام، بستدعي دالة اسمها loginUserApi.

داخل loginUserApi:
بجهز JSON فيه الإيميل والباسوورد.
ببعت الطلب للـ API (POST) باستخدام مكتبة Volley.

لما ييجي الرد من السيرفر:
بتأكد إذا success = true.
لو ناجح، بأخد التوكين (JWT) اللي جاي من السيرفر.

بعدين بفك التوكين باستخدام دالة extractUserInfoFromJwt عشان أجيب:
userId
role (المستخدم أو أدمن).

بخزن هاي المعلومات في SharedPreferences:
التوكين.
userId.
role.

بعد تسجيل الدخول:
لو الدور = admin  بروح على MainActivity كـ أدمن.
لو الدور غير هيك  بروح على MainActivity كمستخدم عادي.
في الحالتين بظهر رسالة Toast إنه تم تسجيل الدخول.
بعد هيك بعمل finish() عشان ما يرجع على صفحة تسجيل الدخول لما يضغط Back.

دالة extractUserInfoFromJwt:
أنا بقسم التوكين لـ 3 أجزاء (header, payload, signature).
بفك ترميز payload من Base64.
بحوله لـ JSON وبستخرج منه الـ userId والـ role.
برجعهم في Pair.
ولو صار خطأ برجع ("", "user").

     */
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val username = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "الرجاء إدخال اسم المستخدم وكلمة المرور", Toast.LENGTH_SHORT).show()
            } else {
                loginUserApi(username, password)
            }
        }

        binding.DontHaveAccount.setOnClickListener {
            startActivity(Intent(this, CreatAccount::class.java))
        }
    }

    private fun loginUserApi(username: String, password: String) {
        val url = Api.login_URL

        val jsonBody = JSONObject()
        jsonBody.put("Email", username)
        jsonBody.put("Password", password)

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                val success = response.optBoolean("success", false)
                if (success) {
                    val token = response.optString("data", "")
                    if (token.isNotEmpty()) {
                        val (userId, role) = extractUserInfoFromJwt(token)

                        val sharedPref = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

                        sharedPref.edit()
                            .putString("token", token)
                            .putString("userId", userId)
                            .putString("role", role)
                            .apply()

// just for shick
//                        Toast.makeText(this, "Token: $token", Toast.LENGTH_LONG).show()
//                        android.util.Log.d("AUTH_TOKEN", token)

                        if (role.equals("admin", ignoreCase = true)) {
                            Toast.makeText(this, "You are logged in as an administrator.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java).apply {
                                putExtra("role", "admin")
                            })
                        } else {
                            
                            Toast.makeText(this, "تم تسجيل الدخول ", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                        }
                        finish()
                    } else {
                        Toast.makeText(this, "لم يتم استلام التوكين", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "فشل تسجيل الدخول: ${response.optString("message")}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(this, "خطأ في الاتصال: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun extractUserInfoFromJwt(token: String): Pair<String, String> {
        try {
            val parts = token.split(".")
            if (parts.size == 3) {
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                val json = JSONObject(payload)
/*
هدول عبارة عن مفاتيح (keys) داخل الـ JWT، معمولين بهيك شكل طويل لأنه جايين من ASP.NET Identity.
واحد فيهم بيرجع UserId، والثاني بيرجع Role.
إنا بتستعملهم عشان اعرف مين المستخدم وشو صلاحياته.
 */
                val userId = json.optString(
                    "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier", ""
                )
                val role = json.optString(
                    "http://schemas.microsoft.com/ws/2008/06/identity/claims/role", "user"
                )

                // نخلي الدور كله lowercase عشان المقارنة أسهل
                return Pair(userId, role.lowercase())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair("", "user")
    }
}
