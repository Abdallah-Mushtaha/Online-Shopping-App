package com.testing.finalprojects.Api

class Api {
    companion object{
// Change-Domain /api/
        const val BASE_URL = "https://9a9f9a863fb9.ngrok-free.app/api/"
        const val PRODUCTS_URL = "${BASE_URL}Products"
        const val CATEGORIES_URL = "${BASE_URL}Categories"
        const val GetAllByCategory_URL = "${BASE_URL}Products/GetAllByCategory"

        const val Profile_URL = "${BASE_URL}User/GetCustomerById"


        //        Rejester / Login
        const val register_URL=   "${BASE_URL}Auth/register"
        const val login_URL=   "${BASE_URL}Auth/login"
//       Search
        const val PRODUCTS_FILTER_URL = "${BASE_URL}Products/filter"
        //Cart
        const val ADD_TO_CART_URL = "${BASE_URL}Cart/add-to-cart"
        const val GET_CART_URL = "${BASE_URL}Cart/get-cart"
        const val REMOVE_FROM_CART_URL = "${BASE_URL}Cart/remove-from-cart"
        // orders
        const val ORDERS_URL = "${BASE_URL}Orders"
        const val GET_ALL_ORDERS_URL = "${BASE_URL}Orders/GetAll"
        const val GET_ALL_ORDERS_DETAILS_URL = "${BASE_URL}Orders/GetAllDetails"
        const val CHECKOUT_URL = "${BASE_URL}Orders/checkout"
        const val GET_ORDERS_BY_CUSTOMER_URL = "${BASE_URL}Orders/GetOrdersByCustomer"
    }

}