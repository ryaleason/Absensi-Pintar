package com.example.absensipintar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.absensipintar.databinding.FragmentSettingBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

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


        b.jamAwal.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                b.jamAwal.setText("${selectedTime}:00")
            }, hour, minute, true)

            timePicker.show()
        }

        b.jamAkhir.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                b.jamAkhir.setText("${selectedTime}:00")
            }, hour, minute, true)

            timePicker.show()
        }

        val alasanArray = lokasiMap.keys.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), R.layout.spiner_item, alasanArray)
        adapter.setDropDownViewResource(R.layout.spiner_dropdown)
        b.spinnerAlasan.adapter = adapter

        b.submit.setOnClickListener {
            if (b.jamAwal.text.isNullOrEmpty() || b.jamAkhir.text.isNullOrEmpty() || b.radius.text.isNullOrEmpty()){
                Toast.makeText(requireContext(), "Isi Form Terlebih Dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                val selectedItem = b.spinnerAlasan.selectedItem.toString().trim()

                if (lokasiMap.containsKey(selectedItem)) {
                    val (latitude, longitude) = lokasiMap[selectedItem]!!

                    val lokasiRef = FirebaseFirestore.getInstance().collection("users")
                        .document("U3KAdLt2qOY9k948AOaqFAZGvvf1").collection("lokasi")
                        .document("6fOQUBZPrU31eRR5uSKS")

                    lokasiRef.update(
                        mapOf(
                            "batasMasukAbsen" to b.jamAwal.text.toString(),
                            "batasPulangAbsen" to b.jamAkhir.text.toString(),
                            "latitude" to latitude.toString(),
                            "longitude" to longitude.toString(),
                            "radius" to b.radius.text.toString()
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(requireContext(), "Data Berhasil Diupdate", Toast.LENGTH_SHORT).show()
                        startActivity(Intent (requireContext(),Admin::class.java))
                    }
                } else {
                    Toast.makeText(requireContext(), "Lokasi tidak ditemukan!", Toast.LENGTH_SHORT).show()
                }
            }



        }

        return b.root
    }
}
