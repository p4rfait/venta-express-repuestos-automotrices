package com.p4rfait.ventaexpressrepuestosautomotrices

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.p4rfait.ventaexpressrepuestosautomotrices.databinding.ActivityUploadCustomerBinding

class UploadCustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadCustomerBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    private var editMode = false
    private var customerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUploadCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, ins ->
            val sb = ins.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom); ins
        }

        editMode = intent.getStringExtra("mode") == "edit"
        if (editMode) {
            customerId = intent.getStringExtra("customerId")
            binding.inputName.setText(intent.getStringExtra("name") ?: "")
            binding.inputEmail.setText(intent.getStringExtra("email") ?: "")
            binding.inputPhone.setText(intent.getStringExtra("phone") ?: "")
            binding.btnSubmit.text = "Editar cliente"
        }

        binding.btnSubmit.setOnClickListener {
            if (editMode) updateCustomer() else createCustomer()
        }
    }

    private fun createCustomer() {
        val (name, email, phone) = readFields() ?: return
        val c = Customer(name, email, phone)
        uid?.let { id ->
            db.collection("users").document(id).collection("customers")
                .add(c)
                .addOnSuccessListener { Toast.makeText(this, "Cliente agregado", Toast.LENGTH_SHORT).show(); finish() }
                .addOnFailureListener { e -> Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
        } ?: noAuth()
    }

    private fun updateCustomer() {
        val docId = customerId ?: run { Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show(); return }
        val (name, email, phone) = readFields() ?: return
        val data = mapOf("name" to name, "email" to email, "phone" to phone)

        uid?.let { id ->
            db.collection("users").document(id).collection("customers").document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener { Toast.makeText(this, "Cliente editado", Toast.LENGTH_SHORT).show(); finish() }
                .addOnFailureListener { e -> Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
        } ?: noAuth()
    }

    private fun readFields(): Triple<String, String, String>? {
        val name = binding.inputName.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val phone = binding.inputPhone.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show(); return null
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show(); return null
        }
        return Triple(name, email, phone)
    }

    private fun noAuth() = Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
}
