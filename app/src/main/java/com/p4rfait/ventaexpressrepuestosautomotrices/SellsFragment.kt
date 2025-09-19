package com.p4rfait.ventaexpressrepuestosautomotrices

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.p4rfait.ventaexpressrepuestosautomotrices.databinding.FragmentSellsBinding

class SellsFragment : Fragment() {

    private var _binding: FragmentSellsBinding? = null
    private val binding get() = _binding!!

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val sales = mutableListOf<Sale>()
    private lateinit var adapter: SellsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentSellsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(v: View, saved: Bundle?) {
        super.onViewCreated(v, saved)

        adapter = SellsAdapter(sales)
        binding.salesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.salesRecyclerView.adapter = adapter

        binding.fab.setOnClickListener {
            startActivity(Intent(requireContext(), UploadSaleActivity::class.java))
        }

        loadSales()
    }

    private fun loadSales() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(uid).collection("sales")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap: QuerySnapshot?, e ->
                if (e != null || snap == null) return@addSnapshotListener
                val fresh = snap.documents.mapNotNull { d ->
                    d.toObject(Sale::class.java)?.apply { id = d.id }
                }
                adapter.replaceAll(fresh)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView(); _binding = null
    }
}
