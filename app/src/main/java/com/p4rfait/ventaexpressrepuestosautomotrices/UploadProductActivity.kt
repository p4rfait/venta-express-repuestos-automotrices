package com.p4rfait.ventaexpressrepuestosautomotrices

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.p4rfait.ventaexpressrepuestosautomotrices.databinding.ActivityUploadProductBinding

class UploadProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadProductBinding
    private val db = FirebaseFirestore.getInstance()
    private val currentUser get() = FirebaseAuth.getInstance().currentUser?.uid

    private var editMode = false
    private var productId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUploadProductBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom); insets
        }

        editMode = intent.getStringExtra("mode") == "edit"
        if (editMode) {
            productId = intent.getStringExtra("productId")
            binding.uploadProductName.setText(intent.getStringExtra("name") ?: "")
            binding.uploadProductDesc.setText(intent.getStringExtra("description") ?: "")
            binding.editTextText3.setText(intent.getDoubleExtra("price", 0.0).toString())
            binding.uploadProductStock.setText(intent.getIntExtra("stock", 0).toString())
            binding.UploadProductButton.text = "Editar producto"
        }

        binding.UploadProductButton.setOnClickListener {
            if (editMode) updateProduct() else uploadProduct()
        }
    }

    private fun uploadProduct() {
        val (name, description, price, stock) = readFields() ?: return
        val product = Product(name, description, price, stock)

        currentUser?.let { uid ->
            db.collection("users").document(uid).collection("products")
                .add(product)
                .addOnSuccessListener {
                    Toast.makeText(this, "Producto agregado", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: toastNoAuth()
    }

    private fun updateProduct() {
        val pid = productId ?: run {
            Toast.makeText(this, "ID de producto inválido", Toast.LENGTH_SHORT).show(); return
        }
        val (name, description, price, stock) = readFields() ?: return
        val data = mapOf(
            "name" to name,
            "description" to description,
            "price" to price,
            "stock" to stock
        )

        currentUser?.let { uid ->
            db.collection("users").document(uid)
                .collection("products").document(pid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener { Toast.makeText(this, "Producto editado", Toast.LENGTH_SHORT).show() }
                .addOnFailureListener { e -> Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
        } ?: toastNoAuth()
    }

    private fun readFields(): Quadruple<String,String,Double,Int>? {
        val name = binding.uploadProductName.text.toString().trim()
        val description = binding.uploadProductDesc.text.toString().trim()
        val price = binding.editTextText3.text.toString().trim().toDoubleOrNull()
        val stock = binding.uploadProductStock.text.toString().trim().toIntOrNull()

        if (name.isEmpty() || description.isEmpty() || price == null || stock == null) {
            Toast.makeText(this, "Completa todos los campos con valores válidos", Toast.LENGTH_SHORT).show()
            return null
        }
        return Quadruple(name, description, price, stock)
    }

    private fun clearFields() {
        binding.uploadProductName.text?.clear()
        binding.uploadProductDesc.text?.clear()
        binding.editTextText3.text?.clear()
        binding.uploadProductStock.text?.clear()
    }

    private fun toastNoAuth() =
        Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
}

data class Quadruple<A,B,C,D>(val first:A,val second:B,val third:C,val fourth:D)
