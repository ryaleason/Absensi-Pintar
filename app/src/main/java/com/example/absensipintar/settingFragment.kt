package com.example.absensipintar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.absensipintar.databinding.FragmentSettingBinding
import com.google.firebase.firestore.FirebaseFirestore

class settingFragment : Fragment() {
    private lateinit var b: FragmentSettingBinding

    private val lokasiMap = mapOf(
        "SMK Negeri 6 Jember" to Pair(-8.155307, 113.435150),
        "Rumah" to Pair(-8.217972, 113.379163),
        "SMK Negeri 8 Jember" to Pair(-8.212767, 113.459315),
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = FragmentSettingBinding.inflate(layoutInflater)

        val alasanArray = lokasiMap.keys.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), R.layout.spiner_item, alasanArray)
        adapter.setDropDownViewResource(R.layout.spiner_dropdown)
        b.spinnerAlasan.adapter = adapter

        b.submit.setOnClickListener {
            val selectedItem = b.spinnerAlasan.selectedItem.toString().trim()

            if (lokasiMap.containsKey(selectedItem)) {
                val (latitude, longitude) = lokasiMap[selectedItem]!!

                val lokasiRef = FirebaseFirestore.getInstance().collection("users")
                    .document("U3KAdLt2qOY9k948AOaqFAZGvvf1").collection("lokasi")
                    .document("6fOQUBZPrU31eRR5uSKS")

                lokasiRef.update(
                    mapOf(
                        "latitude" to latitude.toString(),
                        "longitude" to longitude.toString(),
                        "radius" to b.radius.text.toString()
                    )
                ).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Lokasi berhasil diupdate: $selectedItem", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Lokasi tidak ditemukan!", Toast.LENGTH_SHORT).show()
            }
        }

        return b.root
    }
}
