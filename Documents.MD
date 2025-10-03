# 📘 وثيقة المشروع: تطبيق متجر إلكتروني (E-Commerce App)

## 🔹 1. مقدمة
تم تطوير هذا المشروع ليكون تطبيق متجر إلكتروني (E-Commerce) مبسط يعمل على نظام تشغيل **Android** باستخدام لغة **Kotlin**.  
يوفر التطبيق إمكانية تسجيل الدخول والتسجيل، عرض المنتجات والتصنيفات، إضافة المنتجات إلى السلة، إدارة الطلبات، بالإضافة إلى وضع خاصية الإدارة (Admin Mode) لإدارة المنتجات والتصنيفات.

---

## 🔹 2. الهدف من المشروع
- توفير واجهة سهلة الاستخدام لشراء المنتجات.
- دعم المصادقة باستخدام **JWT Token**.
- دعم دورين للمستخدمين:
    - **المستخدم العادي (Customer)**: تصفح المنتجات، إضافة منتجات للسلة، عمل طلب.
    - **المسؤول (Admin)**: إدارة المنتجات والتصنيفات (إضافة، تعديل، حذف).
- دعم التكامل مع **Google Maps API** لعرض مواقع المنتجات.

---

## 🔹 3. البيئة المستخدمة
- **لغة البرمجة**: Kotlin
- **بيئة التطوير**: Android Studio
- **واجهة المستخدم**: ViewBinding + XML Layouts
- **إصدار Android SDK**:
    - `minSdk = 24`
    - `targetSdk = 35`
    - `compileSdk = 36`

---

## 🔹 4. المكتبات (Dependencies)

```kotlin
// Core
implementation("androidx.core:core-ktx:1.10.1")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.9.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")
implementation("androidx.activity:activity-ktx:1.9.0")

// Navigation
val nav_version = "2.7.2"
implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
implementation("androidx.navigation:navigation-ui-ktx:$nav_version")

// RecyclerView / CardView / Glide / Gson
implementation("androidx.recyclerview:recyclerview:1.3.0")
implementation("androidx.cardview:cardview:1.0.0")
implementation("com.github.bumptech.glide:glide:4.15.1")
kapt("com.github.bumptech.glide:compiler:4.15.1")
implementation("com.google.code.gson:gson:2.10.1")

// Networking
implementation("com.android.volley:volley:1.2.1")
implementation("com.squareup.okhttp3:okhttp:4.10.0")

// Google Play Services (Maps + Location)
implementation("com.google.android.gms:play-services-maps:18.2.0")
implementation("com.google.android.gms:play-services-location:21.0.1")

// Testing
testImplementation("junit:junit:4.13.2")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
```
مكتبة Glide

مكتبة قوية لتحميل وعرض الصور من الإنترنت أو الذاكرة.

تدعم الكاش (Caching) لتسريع عرض الصور.

سهلة الدمج مع RecyclerView أو ImageView.

 Glide Compiler (Annotation Processor)

تُستخدم مع Glide لتوليد كود مخصص أثناء الـ compile-time.
تساعد في تحسين الأداء وتمكين ميزات إضافية (زي GlideApp).

 مكتبة Gson (من Google)

لتحويل البيانات بين JSON ↔ Objects (Java/Kotlin).

مثال: لما تجيب بيانات من API بصيغة JSON، تقدر تستخدم Gson لتحويلها مباشرة إلى كائن (Model Class).

---

## 🔹 5. بنية المشروع (Structure)

```
com.testing.finalprojects
│
├── Api
│   └── Api.kt                 // روابط الـ Endpoints
│
├── Model
│   ├── Category.kt             // موديل التصنيف
│   ├── Product.kt              // موديل المنتج
│   └── User.kt                 // موديل المستخدم
│
├── fragments
│   ├── ViewProductsCategory.kt // عرض المنتجات ضمن تصنيف
│   ├── CartFragment.kt         // إدارة السلة
│   ├── OrdersFragment.kt       // عرض الطلبات
│   └── AdminDashboardFragment.kt
│
├── adapter
│   ├── CategoryAdapter.kt      // RecyclerView للتصنيفات
│   └── ProductAdapter.kt       // RecyclerView للمنتجات
│
├── LoginActivity.kt            // تسجيل الدخول
├── CreatAccount.kt             // إنشاء حساب
└── MainActivity.kt             // إدارة الواجهات والملاحة
```

---

## 🔹 6. الـ Models

### ✅ Category
```kotlin
data class Category(
    var id: Long,
    var name: String,
    var image: String = "drawable/trash.png"
)
```

### ✅ Product
```kotlin
data class Product(
    val id: String,
    var name: String,
    var description: String = "",
    var imageUrl: String = "",
    var price: Double,
    val rate: Float = 0.0f,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var category: String? = null,
    var locationName: String = "",
    val location: String? = null,
    val categoryId: Int,
    var quantity: Int = 1,
    val image: String
)
```

### ✅ User
```kotlin
data class User(
    val id: String,
    val email: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    val phone: String?,
    val role: String
)
```

---

## 🔹 7. المصادقة (Authentication)

### 🔑 تسجيل الدخول (LoginActivity)
- إرسال البريد الإلكتروني وكلمة المرور للـ API.
- استقبال **JWT Token**.
- استخراج **UserId** و **Role** من الـ Token.

```kotlin
val userId = json.optString("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier", "")
val role = json.optString("http://schemas.microsoft.com/ws/2008/06/identity/claims/role", "user")
```

---

## 🔹 8. واجهة المستخدم (UI)

- **BottomNavigationView**:
    - وضع خاص بالـ Admin → يظهر أزرار (Dashboard, Add Product, Manage Categories).
    - وضع خاص بالـ Customer → يظهر أزرار (Home, Cart, Orders).

- **RecyclerView + Adapter**:
    - `CategoryAdapter` لعرض التصنيفات.
    - `ProductAdapter` لعرض المنتجات مع دعم أوضاع الأزرار:
        - **CLIENT_HOME** → زر (Add).
        - **CLIENT_CART** → أزرار زيادة/إنقاص الكمية + حذف.
        - **ADMIN** → زر (Delete).

---

## 🔹 9. Google Maps Integration
- عرض موقع المنتج على الخريطة باستخدام **Lat / Lng**.
- تم استخدام:
```kotlin
implementation("com.google.android.gms:play-services-maps:18.2.0")
implementation("com.google.android.gms:play-services-location:21.0.1")
```
- التنقل من المنتج → فتح الخريطة مع تمرير `lat` و `lng`.

---

## 🔹 10. السلة والطلبات (Cart & Orders)
- السلة تتم إدارتها باستخدام `SharedPreferences` (محليًا).
- الطلبات تتم من خلال استدعاء API:
    - `Cart/add-to-cart`
    - `Cart/get-cart`
    - `Orders/checkout`
    - `Orders/GetOrdersByCustomer`

---

## 🔹 11. الخاتمة
هذا المشروع يجمع بين:
- **إدارة الحسابات والمصادقة باستخدام JWT**.
- **إدارة المنتجات والتصنيفات**.
- **استخدام Google Maps API**.
- **وضعيتين للمستخدم (Admin / Customer)**.

وبالتالي يعتبر مشروع متكامل لتطبيق متجر إلكتروني مصغر.
