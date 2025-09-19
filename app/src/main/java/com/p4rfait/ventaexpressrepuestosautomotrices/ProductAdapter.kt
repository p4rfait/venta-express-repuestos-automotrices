package com.p4rfait.ventaexpressrepuestosautomotrices

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.p4rfait.ventaexpressrepuestosautomotrices.databinding.RecyclerProductBinding
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private val items: MutableList<Product>,
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.VH>() {

    init {
        setHasStableIds(true)
    }

    inner class VH(val binding: RecyclerProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RecyclerProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]

        // Formateo de precio con la configuración regional del dispositivo
        val currency = NumberFormat.getCurrencyInstance(Locale.getDefault())
        val priceText = currency.format(p.price)

        with(holder.binding) {
            recProductName.text  = p.name
            recProductDesc.text  = p.description
            recProductStock.text = "Cantidad: ${p.stock}"
            recProductPrice.text = "Precio: $priceText"

            btnEdit.setOnClickListener   { onEditClick(p) }
            btnDelete.setOnClickListener { onDeleteClick(p) }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        val id = items[position].id
        return id?.hashCode()?.toLong() ?: items[position].hashCode().toLong()
    }

    fun submitList(newItems: List<Product>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = items[oldItemPosition]
                val newItem = newItems[newItemPosition]
                return if (oldItem.id != null && newItem.id != null) {
                    oldItem.id == newItem.id
                } else {
                    oldItem.name == newItem.name &&
                            oldItem.description == newItem.description &&
                            oldItem.price == newItem.price &&
                            oldItem.stock == newItem.stock
                }
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = items[oldItemPosition]
                val newItem = newItems[newItemPosition]
                return oldItem == newItem
            }
        })

        items.clear()
        items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }
}
