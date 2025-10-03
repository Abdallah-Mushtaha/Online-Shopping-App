# 📘 Project Document: E-Commerce App

## 🔹 1. Introduction

This project was developed as a simplified **E-Commerce** application
running on **Android** using **Kotlin**.\
The app provides login and registration, product and category display,
adding products to the cart, order management, as well as an **Admin
Mode** for managing products and categories.

---

## 🔹 2. Project Objective

- Provide an easy-to-use interface for purchasing products.\
- Support authentication using **JWT Token**.\
- Support two user roles:
  - **Customer**: Browse products, add products to cart, place
    orders.\
  - **Admin**: Manage products and categories (add, edit, delete).\
- Support integration with **Google Maps API** to display product
  locations.

---

## 🔹 3. Environment

- **Programming Language**: Kotlin\
- **IDE**: Android Studio\
- **UI**: ViewBinding + XML Layouts\
- **Android SDK Versions**:
  - `minSdk = 24`\
  - `targetSdk = 35`\
  - `compileSdk = 36`

---

## 🔹 4. Dependencies

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

**Glide Library**\
A powerful library for loading and displaying images from the internet
or memory.\
Supports caching to speed up image display.\
Easily integrates with RecyclerView or ImageView.

**Glide Compiler (Annotation Processor)**\
Used with Glide to generate custom code at compile-time.\
Helps improve performance and enable additional features (like
GlideApp).

**Gson Library (from Google)**\
For converting data between JSON ↔ Objects (Java/Kotlin).\
Example: When fetching data from an API in JSON format, Gson can
directly convert it into a Model Class.

---

## 🔹 5. Project Structure

    com.testing.finalprojects
    │
    ├── Api
    │   └── Api.kt                 // Endpoints
    │
    ├── Model
    │   ├── Category.kt             // Category model
    │   ├── Product.kt              // Product model
    │   └── User.kt                 // User model
    │
    ├── fragments
    │   ├── ViewProductsCategory.kt // Show products by category
    │   ├── CartFragment.kt         // Manage cart
    │   ├── OrdersFragment.kt       // Show orders
    │   └── AdminDashboardFragment.kt
    │
    ├── adapter
    │   ├── CategoryAdapter.kt      // RecyclerView for categories
    │   └── ProductAdapter.kt       // RecyclerView for products
    │
    ├── LoginActivity.kt            // Login
    ├── CreatAccount.kt             // Create account
    └── MainActivity.kt             // Manage UI and navigation

---

## 🔹 6. Models

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

## 🔹 7. Authentication

### 🔑 Login (LoginActivity)

- Send email and password to the API.\
- Receive **JWT Token**.\
- Extract **UserId** and **Role** from the token.

```kotlin
val userId = json.optString("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier", "")
val role = json.optString("http://schemas.microsoft.com/ws/2008/06/identity/claims/role", "user")
```

---

## 🔹 8. User Interface (UI)

- **BottomNavigationView**:
  - Admin Mode → shows (Dashboard, Add Product, Manage Categories).\
  - Customer Mode → shows (Home, Cart, Orders).
- **RecyclerView + Adapter**:
  - `CategoryAdapter` to display categories.\
  - `ProductAdapter` to display products with button modes:
    - **CLIENT_HOME** → Add button.\
    - **CLIENT_CART** → Increase/Decrease quantity + Delete.\
    - **ADMIN** → Delete button.

---

## 🔹 9. Google Maps Integration

- Display product location on the map using **Lat / Lng**.\
- Using:\

```kotlin
implementation("com.google.android.gms:play-services-maps:18.2.0")
implementation("com.google.android.gms:play-services-location:21.0.1")
```

- Navigate from product → open map with `lat` and `lng`.

---

## 🔹 10. Cart & Orders

- Cart is managed using `SharedPreferences` (locally).\
- Orders are managed via API calls:
  - `Cart/add-to-cart`\
  - `Cart/get-cart`\
  - `Orders/checkout`\
  - `Orders/GetOrdersByCustomer`

---

## 🔹 11. Conclusion

This project combines:\

- **Account management and JWT authentication**.\
- **Product and category management**.\
- **Google Maps API integration**.\
- **Two user modes (Admin / Customer)**.

Thus, it represents a complete mini e-commerce application.

This application was developed by Eng.Abdullah Mushtaha and Eng. Moath Ashour
under the supervision of Eng.Hiyam Abu Hasirah.
