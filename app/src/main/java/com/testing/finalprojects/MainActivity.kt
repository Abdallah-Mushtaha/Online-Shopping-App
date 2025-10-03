package com.testing.finalprojects

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.onlineshop.LoginActivity
import com.testing.finalprojects.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    /*
أنا عندي MainActivity، وهي الشاشة الرئيسية اللي بتتحكم في كل التصفح داخل التطبيق.
أنا عامل فيها شرط: إذا أنا كنت أدمن، بظهرلي شريط تنقل (BottomNavigation) خاص بالأدمن، أما إذا كنت زبون (Customer) بظهرلي شريط مختلف خاص بالزبون.
لما أعمل تسجيل دخول، الدور (Role) بيجيني من LoginActivity وبوصله للـ MainActivity. إذا الدور طلع "admin"، أنا بظهر واجهة الأدمن وبفتحلي شاشة لوحة التحكم (Dashboard) مباشرة. إذا مش أدمن، أنا بظهر واجهة الزبون.
كمان، أنا مبرمج خيارات الأدمن في الـ BottomNavigation:
إذا اخترت "Dashboard" بروح لشاشة التحكم.
إذا اخترت "Add Product" بروح على شاشة إضافة منتجات.
إذا اخترت "Show Products" بروح على شاشة عرض المنتجات.
وإذا اخترت "Logout"، أنا برجعني على شاشة تسجيل الدخول وبسكر الـ MainActivity.
     */
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // تمكين EdgeToEdge
        enableEdgeToEdge()

        // التعامل مع SystemBars
        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        val role = intent.getStringExtra("role") ?: "customer"

        // NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        if (role == "admin") {
            binding.bottomNavigationAdmin.visibility = View.VISIBLE
            binding.bottomNavigationClient.visibility = View.GONE
            binding.bottomNavigationAdmin.setupWithNavController(navController)

            // الانتقال مباشرة للفراجمنت الخاص بالـ Admin
            navController.navigate(R.id.adminDashboardFragment)
        } else {
            binding.bottomNavigationAdmin.visibility = View.GONE
            binding.bottomNavigationClient.visibility = View.VISIBLE
            binding.bottomNavigationClient.setupWithNavController(navController)
        }

        binding.bottomNavigationAdmin.setOnNavigationItemSelectedListener() { menuItem ->
            when(menuItem.itemId) {
                R.id.adminDashboardFragment -> {
                    // الانتقال لفراجمنت لوحة تحكم الأدمن
                    findNavController(R.id.nav_host_fragment).navigate(R.id.adminDashboardFragment)
                }
                R.id.addProductFragment -> {
                    // الانتقال لفراجمنت إضافة منتجات
                    findNavController(R.id.nav_host_fragment).navigate(R.id.addProductFragment)
                }
                R.id.admin_show_productes -> {
                    // الانتقال لفراجمنت عرض منتجات
                    findNavController(R.id.nav_host_fragment).navigate(R.id.admin_show_productes)
                }
                R.id.logout -> {
                    // تسجيل الخروج
                    val intent = Intent(this, com.example.onlineshop.LoginActivity::class.java)
                    startActivity(intent)
                    finish() // إغلاق الـ Activity الحالي
                }
            }
            true
        }


    }

}
