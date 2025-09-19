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
import com.p4rfait.ventaexpressrepuestosautomotrices.databinding.FragmentCustomersBinding

class CustomersFragment : Fragment() {

    private var _binding: FragmentCustomersBinding? = null
    private val binding get() = _binding!!

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val customerList = mutableListOf<Customer>()
    private lateinit var adapter: CustomerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CustomerAdapter(
            items = customerList,
            onEditClick = { customer -> openEdit(customer) },
            onDeleteClick = { customer -> confirmAndDelete(customer) }
        )

        binding.customerRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CustomersFragment.adapter
            setHasFixedSize(true)
        }

        binding.fab.setOnClickListener {
            startActivity(Intent(requireActivity(), UploadCustomerActivity::class.java))
        }

        loadCustomers()
    }

    private fun loadCustomers() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(uid).collection("customers")
            .addSnapshotListener { snapshot: QuerySnapshot?, e: FirebaseFirestoreException? ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                customerList.clear()
                for (doc in snapshot.documents) {
                    val c = doc.toObject(Customer::class.java)
                    if (c != null) {
                        c.id = doc.id
                        customerList.add(c)
                    }
                }
                // Si usas emptyView en tu XML, puedes alternarlo aquí:
                // binding.emptyView.visibility = if (customerList.isEmpty()) View.VISIBLE else View.GONE

                adapter.notifyDataSetChanged()
            }
    }

    private fun openEdit(customer: Customer) {
        val intent = Intent(requireContext(), UploadCustomerActivity::class.java).apply {
            putExtra("mode", "edit")
            putExtra("customerId", customer.id)
            putExtra("name", customer.name)
            putExtra("email", customer.email)
            putExtra("phone", customer.phone)
        }
        startActivity(intent)
    }

    private fun confirmAndDelete(customer: Customer) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar cliente")
            .setMessage("¿Seguro que deseas eliminar a \"${customer.name}\"?")
            .setPositiveButton("Eliminar") { _, _ -> deleteCustomer(customer) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCustomer(customer: Customer) {
        val uid = auth.currentUser?.uid ?: return
        val id = customer.id ?: return

        db.collection("users").document(uid)
            .collection("customers").document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Cliente eliminado", Toast.LENGTH_SHORT).show()
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
