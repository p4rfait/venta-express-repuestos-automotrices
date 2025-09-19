package com.p4rfait.ventaexpressrepuestosautomotrices

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.p4rfait.ventaexpressrepuestosautomotrices.databinding.FragmentProductsBinding

class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val productList = mutableListOf<Product>()
    private lateinit var adapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProductAdapter(
            items = productList,
            onEditClick = { product -> openEdit(product) },
            onDeleteClick = { product -> confirmAndDelete(product) }
        )

        binding.productRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ProductsFragment.adapter
            setHasFixedSize(true)
        }

        binding.fab.setOnClickListener {
            startActivity(Intent(requireActivity(), UploadProductActivity::class.java))
        }

        loadProducts()
    }

    private fun loadProducts() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(uid).collection("products")
            .addSnapshotListener { snapshot: QuerySnapshot?, e: FirebaseFirestoreException? ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                productList.clear()
                for (doc in snapshot.documents) {
                    val p = doc.toObject(Product::class.java)
                    if (p != null) {
                        p.id = doc.id
                        productList.add(p)
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun openEdit(product: Product) {
        val intent = Intent(requireContext(), UploadProductActivity::class.java).apply {
            putExtra("mode", "edit")
            putExtra("productId", product.id)
            putExtra("name", product.name)
            putExtra("description", product.description)
            putExtra("price", product.price)
            putExtra("stock", product.stock)
        }
        startActivity(intent)
    }

    private fun confirmAndDelete(product: Product) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar producto")
            .setMessage("¿Seguro que deseas eliminar \"${product.name}\"?")
            .setPositiveButton("Eliminar") { _, _ -> deleteProduct(product) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteProduct(product: Product) {
        val uid = auth.currentUser?.uid ?: return
        val productId = product.id ?: return

        db.collection("users").document(uid)
            .collection("products").document(productId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Producto eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
