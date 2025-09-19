package com.p4rfait.ventaexpressrepuestosautomotrices

import com.google.firebase.firestore.Exclude

data class SaleItem(
    val productId: String = "",
    val productName: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 0,
    val subtotal: Double = 0.0
)

data class Sale(
    val customerId: String = "",
    val customerName: String = "",
    val items: List<SaleItem> = emptyList(),
    val total: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    @get:Exclude @set:Exclude var id: String? = null
)
