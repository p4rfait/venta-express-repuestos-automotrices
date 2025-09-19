package com.p4rfait.ventaexpressrepuestosautomotrices

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.p4rfait.ventaexpressrepuestosautomotrices.databinding.RecyclerSaleBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class SellsAdapter(
    private val items: MutableList<Sale>
) : RecyclerView.Adapter<SellsAdapter.VH>() {

    inner class VH(val binding: RecyclerSaleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = RecyclerSaleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        val currency = NumberFormat.getCurrencyInstance()
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        with(holder.binding) {
            saleCustomer.text = "Cliente: ${s.customerName}"
            saleDate.text = "Fecha: ${df.format(Date(s.createdAt))}"
            val itemsText = s.items.joinToString(separator = "\n") { "• ${it.productName} x${it.quantity}" }
            saleItemsSummary.text = itemsText
            saleTotal.text = "Total: ${currency.format(s.total)}"
        }
    }

    override fun getItemCount(): Int = items.size

    fun replaceAll(newItems: List<Sale>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
