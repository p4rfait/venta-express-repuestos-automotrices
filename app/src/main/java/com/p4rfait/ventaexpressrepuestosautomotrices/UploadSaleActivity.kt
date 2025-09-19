package com.p4rfait.ventaexpressrepuestosautomotrices

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.p4rfait.ventaexpressrepuestosautomotrices.databinding.ActivityUploadSaleBinding
import java.text.NumberFormat

class UploadSaleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadSaleBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    private val products = mutableListOf<Product>()  // name, price, stock, id
    private val customers = mutableListOf<Customer>() // name, email, phone, id
    private val currency = NumberFormat.getCurrencyInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUploadSaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCustomers()
        loadProducts()

        binding.btnAddItem.setOnClickListener { addItemRow() }
        binding.btnSubmitSale.setOnClickListener { submitSale() }

        addItemRow()
    }

    private fun loadCustomers() {
        val id = uid ?: return
        db.collection("users").document(id).collection("customers")
            .get().addOnSuccessListener { snap ->
                customers.clear()
                customers.addAll(snap.documents.mapNotNull { d ->
                    d.toObject(Customer::class.java)?.apply { this.id = d.id }
                })
                val names = customers.map { it.name }
                binding.spinnerCustomer.adapter = ArrayAdapter(
                    this, android.R.layout.simple_spinner_dropdown_item, names
                )
            }
    }

    private fun loadProducts() {
        val id = uid ?: return
        db.collection("users").document(id).collection("products")
            .get().addOnSuccessListener { snap ->
                products.clear()
                products.addAll(snap.documents.mapNotNull { d ->
                    d.toObject(Product::class.java)?.apply { this.id = d.id }
                })
                refreshAllItemSpinners()
            }
    }

    private fun refreshAllItemSpinners() {
        val names = products.map { it.name }
        for (row in binding.containerItems.children) {
            val spinner = row.findViewById<Spinner>(R.id.spinnerProduct)
            spinner.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_dropdown_item, names
            )
        }
    }

    private fun addItemRow() {
        val row = LayoutInflater.from(this).inflate(R.layout.row_sale_item, binding.containerItems, false)
        val spinner = row.findViewById<Spinner>(R.id.spinnerProduct)
        val inputQty = row.findViewById<EditText>(R.id.inputQty)
        val textSubtotal = row.findViewById<TextView>(R.id.textSubtotal)

        val names = products.map { it.name }
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, names
        )

        val recalc = {
            val product = products.getOrNull(spinner.selectedItemPosition)
            val qty = inputQty.text.toString().toIntOrNull() ?: 0
            val subtotal = if (product != null) product.price * qty else 0.0
            textSubtotal.text = currency.format(subtotal)
            recomputeTotal()
        }

        spinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) = recalc()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
        inputQty.addTextChangedListener(SimpleTextWatcher { recalc() })

        binding.containerItems.addView(row)
        recalc()
    }

    private fun recomputeTotal() {
        var total = 0.0
        for (row in binding.containerItems.children) {
            val spinner = row.findViewById<Spinner>(R.id.spinnerProduct)
            val inputQty = row.findViewById<EditText>(R.id.inputQty)
            val product = products.getOrNull(spinner.selectedItemPosition)
            val qty = inputQty.text.toString().toIntOrNull() ?: 0
            if (product != null && qty > 0) total += product.price * qty
        }
        binding.textTotal.text = "Total: ${currency.format(total)}"
    }

    private fun submitSale() {
        val userId = uid ?: run { toast("Usuario no autenticado"); return }
        if (customers.isEmpty()) { toast("Primero agrega un cliente"); return }
        if (products.isEmpty()) { toast("Primero agrega productos"); return }

        val customerIndex = binding.spinnerCustomer.selectedItemPosition
        if (customerIndex !in customers.indices) { toast("Selecciona un cliente"); return }
        val customer = customers[customerIndex]

        // Construir items
        val saleItems = mutableListOf<SaleItem>()
        for (row in binding.containerItems.children) {
            val spinner = row.findViewById<Spinner>(R.id.spinnerProduct)
            val inputQty = row.findViewById<EditText>(R.id.inputQty)
            val product = products.getOrNull(spinner.selectedItemPosition) ?: continue
            val qty = inputQty.text.toString().toIntOrNull() ?: 0
            if (qty <= 0) continue
            saleItems.add(
                SaleItem(
                    productId = product.id ?: "",
                    productName = product.name,
                    unitPrice = product.price,
                    quantity = qty,
                    subtotal = product.price * qty
                )
            )
        }
        if (saleItems.isEmpty()) { toast("Agrega al menos un producto con cantidad"); return }
        val total = saleItems.sumOf { it.subtotal }

        val userRef = db.collection("users").document(userId)
        db.runTransaction { tr ->
            val qtyByProduct = mutableMapOf<String, Int>()
            val itemByProduct = mutableMapOf<String, SaleItem>()
            for (it in saleItems) {
                val pid = it.productId
                qtyByProduct[pid] = (qtyByProduct[pid] ?: 0) + it.quantity
                if (!itemByProduct.containsKey(pid)) itemByProduct[pid] = it
            }

            val prodRefs = qtyByProduct.keys.map { pid -> userRef.collection("products").document(pid) }
            val prodSnaps = prodRefs.associateWith { ref -> tr.get(ref) }

            prodSnaps.forEach { (ref, snap) ->
                if (!snap.exists()) throw IllegalStateException("Producto no encontrado: ${ref.id}")
                val currentStock = (snap.getLong("stock") ?: 0L).toInt()
                val required = qtyByProduct[ref.id] ?: 0
                if (required > currentStock) {
                    val name = itemByProduct[ref.id]?.productName ?: "Producto ${ref.id}"
                    throw IllegalStateException("Stock insuficiente para $name (disp: $currentStock, req: $required)")
                }
            }

            prodSnaps.forEach { (ref, snap) ->
                val currentStock = (snap.getLong("stock") ?: 0L).toInt()
                val required = qtyByProduct[ref.id] ?: 0
                tr.update(ref, "stock", currentStock - required)
            }

            val sale = Sale(
                customerId = customer.id ?: "",
                customerName = customer.name,
                items = saleItems,
                total = total,
                createdAt = System.currentTimeMillis()
            )
            tr.set(userRef.collection("sales").document(), sale)

            null
        }.addOnSuccessListener {
            toast("Venta registrada")
            finish()
        }.addOnFailureListener { e ->
            toast("Error: ${e.message}")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

class SimpleTextWatcher(private val onChange: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onChange() }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
