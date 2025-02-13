package com.example.absensipintar

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.absensipintar.databinding.FragmentAjukanIzinBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AjukanIzinFragment : Fragment() {
    private lateinit var b : FragmentAjukanIzinBinding
    private var Image: Bitmap? = null
    private var tanggalAwal: String? = null
    private var tanggalAkhir: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var image: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
         b = FragmentAjukanIzinBinding.inflate(layoutInflater)
            firestore = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
        val nama = requireContext().getSharedPreferences("DATANAMA", Context.MODE_PRIVATE).getString("NAMA","");
        b.EditTextNama.setText("${nama}")

        b.klickKamera.setOnClickListener {
            bukacamera()
        }

        b.EditTextDate.setOnClickListener {
            ambiltanggal()
        }

        b.ajukanIzin.setOnClickListener {
            Image?.let {
                saveimagelokal(it)
            } ?: Toast.makeText(requireContext(), "Ambil foto terlebih dahulu!", Toast.LENGTH_SHORT).show()

            if (image == null){
                Toast.makeText(requireContext(), "Ambil Foto Terlebih dahulu", Toast.LENGTH_SHORT).show()
            }else{
            simpan()
            }

        }

        b.alasan.text

        return b.root

    }

    private fun simpan() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User belum login!", Toast.LENGTH_SHORT).show()
            return
        }

        val nama = b.EditTextNama.text.toString().trim()
        val alasan = b.alasan.text.toString().trim()

        if (nama.isEmpty() || alasan.isEmpty() || tanggalAwal == null || tanggalAkhir == null || image == null) {
            Toast.makeText(requireContext(), "Semua data harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        val izinData = hashMapOf(
            "nama" to nama,
            "alasan" to alasan,
            "tanggal_awal" to tanggalAwal,
            "tanggal_akhir" to tanggalAkhir,
            "tanggal_pengajuan" to currentDate,
            "foto_path" to image
        )

        firestore.collection("users").document(userId)
            .collection("Izin")
            .add(izinData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Izin berhasil diajukan!", Toast.LENGTH_SHORT).show()
            }
    }



    private fun ambiltanggal() {
        val calendar = Calendar.getInstance()
        val tahun = calendar.get(Calendar.YEAR)
        val bulan = calendar.get(Calendar.MONTH)
        val hari = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerAwal = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            tanggalAwal = String.format(Locale.getDefault(), "%02d-%02d-%d", selectedDay, selectedMonth + 1, selectedYear)

            val datePickerAkhir = DatePickerDialog(requireContext(), { _, selectedYearEnd, selectedMonthEnd, selectedDayEnd ->
                tanggalAkhir = String.format(Locale.getDefault(), "%02d-%02d-%d", selectedDayEnd, selectedMonthEnd + 1, selectedYearEnd)

                b.EditTextDate.setText("Dari: $tanggalAwal - Sampai: $tanggalAkhir")

            }, tahun, bulan, hari)

            datePickerAkhir.show()

        }, tahun, bulan, hari)

        datePickerAwal.show()
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            Image = imageBitmap
            b.klickKamera.visibility = View.GONE
            b.imageView.visibility = View.VISIBLE
            b.imageView.setImageBitmap(imageBitmap)

            image = saveimagelokal(imageBitmap)
        }
    }

    private fun bukacamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    private fun saveimagelokal(bitmap: Bitmap): String? {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val fileName = "IMG_$dateFormat.jpg"

        val directory = File(requireContext().filesDir, "IzinImages")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, fileName)

        return try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
            }
            Log.d("FILE", "Gambar disimpan di: ${file.absolutePath}")
            Toast.makeText(requireContext(), "Gambar Tersimpan!", Toast.LENGTH_LONG).show()
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
            null
        }
    }

}