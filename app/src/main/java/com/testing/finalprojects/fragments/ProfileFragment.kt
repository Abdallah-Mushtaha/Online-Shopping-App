package com.testing.finalprojects.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.onlineshop.LoginActivity
import com.google.gson.Gson
import com.testing.finalprojects.Api.Api
import com.testing.finalprojects.Model.User
import com.testing.finalprojects.R
import com.testing.finalprojects.databinding.FragmentProfileBinding
import org.json.JSONObject

class ProfileFragment : Fragment() {
    /*
    مسؤول عن صفحة البروفايل (الحساب الشخصي)
    جلب بيانات المستخدم من السيرفر
يقرأ token من SharedPreferences.
إذا التوكن مفقود  يعرض "غير معروف".

إذا موجود يرسل طلب GET لـ Api.Profile_URL ومعاه Header فيه
Authorization: Bearer <token>
إذا السيرفر رجّع نجاح يحوّل بيانات data إلى كائن User (باستخدام Gson) ويعرضها في الشاشة (tvName, tvEmail, tvPhone).

زر تسجيل الخروج
يمسح التوكن من SharedPreferences.
يفتح شاشة تسجيل الدخول (LoginActivity) وينهي (finish) الـ Activity الحالي.

زر المشتريات (Purchased)
يستخدم Navigation ويروح لـ PurchasedFragment.

---------------------------------------
عرض بيانات مستخدم
أول مرة عندd كود يجيب بيانات User من API ويعرضها (الاسم، الإيميل، الهاتف).
قبل كنت اتعامل مع Products, Categories, Cart.

 (Session Management)
إذا التوكن مفقود أو غير صحيح  يظهر "غير معروف".
إذا عملت Logout امسح التوكن ورجوع المستخدم لواجهة تسجيل الدخول.

Gson مع كائن واحد
قبل استخدمتوا مع Lists (TypeToken<List<Product>>).

هنا يستخدم
val user = Gson().fromJson(dataObj.toString(), User::class.java)

لتحويل JSON Object واحد إلى User.
تبديل واجهة المستخدم حسب الحالة
displayUserData(user) لو في بيانات صحيحة.
showUnknownUser() لو ما في توكن أو خطأ.


     */

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authPrefFile = "auth_prefs"
    private val apiUrl = Api.Profile_URL

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)


        fetchUserDataFromApi()


        binding.logout.setOnClickListener {
            logoutUser()
        }


        binding.Purchased.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_purchasedFragment)
        }

        return binding.root
    }


    private fun fetchUserDataFromApi() {
        val sharedPref = requireContext().getSharedPreferences(authPrefFile, Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", null)

        if (token.isNullOrEmpty()) {
            showUnknownUser()
            return
        }

        val request = object : JsonObjectRequest(
            Request.Method.GET, apiUrl, null,
            { response ->
                try {
                    if (response.optBoolean("success", false)) {
                        val dataObj = response.optJSONObject("data")
                        if (dataObj != null) {
                            val user = Gson().fromJson(dataObj.toString(), User::class.java)
                            displayUserData(user)
                        }
                    } else {
                        Toast.makeText(requireContext(), "فشل: ${response.optString("message")}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "خطأ في معالجة البيانات", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ProfileFragment", "API Error: ${error.message}")
                Toast.makeText(requireContext(), "خطأ في الاتصال", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        Volley.newRequestQueue(requireContext()).add(request)
    }


    private fun displayUserData(user: User) {
        binding.tvName.text = "${user.firstName} ${user.lastName}"
        binding.tvEmail.text = user.email ?: "غير محدد"
        binding.tvPhone.text = user.phone ?: "غير محدد"

    }

    private fun showUnknownUser() {
        binding.tvName.text = "غير معروف"
        binding.tvEmail.text = ""
        binding.tvPhone.text = ""

    }

    private fun logoutUser() {
        val sharedPref = requireContext().getSharedPreferences(authPrefFile, Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}