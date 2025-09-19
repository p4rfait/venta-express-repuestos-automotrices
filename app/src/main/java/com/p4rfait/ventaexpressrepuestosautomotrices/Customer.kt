package com.p4rfait.ventaexpressrepuestosautomotrices

import com.google.firebase.firestore.Exclude

data class Customer(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    @get:Exclude @set:Exclude var id: String? = null
)
