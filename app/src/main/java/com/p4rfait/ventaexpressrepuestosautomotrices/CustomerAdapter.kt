package com.p4rfait.ventaexpressrepuestosautomotrices

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.p4rfait.ventaexpressrepuestosautomotrices.databinding.RecyclerCustomerBinding

class CustomerAdapter(
    private val items: MutableList<Customer>,
    private val onEditClick: (Customer) -> Unit,
    private val onDeleteClick: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.VH>() {

    init { setHasStableIds(true) }

    inner class VH(val binding: RecyclerCustomerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = RecyclerCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        with(holder.binding) {
            recCustomerName.text  = c.name
            recCustomerEmail.text = c.email
            recCustomerPhone.text = c.phone
            btnEdit.setOnClickListener   { onEditClick(c) }
            btnDelete.setOnClickListener { onDeleteClick(c) }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long =
        items[position].id?.hashCode()?.toLong() ?: items[position].hashCode().toLong()

    fun submitList(newItems: List<Customer>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(o: Int, n: Int): Boolean {
                val a = items[o]; val b = newItems[n]
                return if (a.id != null && b.id != null) a.id == b.id
                else a.name == b.name && a.email == b.email && a.phone == b.phone
            }
            override fun areContentsTheSame(o: Int, n: Int): Boolean = items[o] == newItems[n]
        })
        items.clear(); items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }
}
