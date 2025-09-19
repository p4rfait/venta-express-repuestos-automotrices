package com.p4rfait.ventaexpressrepuestosautomotrices

import com.google.firebase.firestore.Exclude

data class Product(
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    @get:Exclude @set:Exclude var id: String? = null // id del documento
)
