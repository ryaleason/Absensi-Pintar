package com.example.absensipintar

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.absensipintar.databinding.FragmentSettingBinding
import com.google.firebase.firestore.FirebaseFirestore

class settingFragment : Fragment() {
    private lateinit var b : FragmentSettingBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        b = FragmentSettingBinding.inflate(layoutInflater)

        val alasanArray = resources.getStringArray(R.array.lokasi_absen)

        val adapter = ArrayAdapter(requireContext(), R.layout.spiner_item, alasanArray)
        adapter.setDropDownViewResource(R.layout.spiner_dropdown)

        val alasan = b.spinnerAlasan.selectedItem.toString().trim()

        b.submit.setOnClickListener {
            val selectedItem = b.spinnerAlasan.selectedItem.toString().trim()

            val lokasiRef = FirebaseFirestore.getInstance().collection("lokasi_absen").document("lokasi_terpilih")
            val lokasiData = hashMapOf(
                "nama" to selectedItem,

            )

            lokasiRef.set(lokasiData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Lokasi absen berhasil diubah!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Gagal menyimpan lokasi ke Firebase!", Toast.LENGTH_SHORT).show()
                }
        }
        b.spinnerAlasan.adapter = adapter
        return b.root
    }


}