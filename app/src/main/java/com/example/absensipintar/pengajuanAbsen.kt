package com.example.absensipintar

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.absensipintar.databinding.ActivityPengajuanAbsenBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class pengajuanAbsen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var imagePath: String? = null

    private lateinit var b : ActivityPengajuanAbsenBinding
    private var Image: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPengajuanAbsenBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val nama = intent.getStringExtra("NAMA")

        b.back.setOnClickListener {
            finish()
        }

        b.klickKamera.setOnClickListener {
            openCamera()
        }



        b.EditTextNama.setText("${nama}")

        b.submit.setOnClickListener {
            Image?.let {
                imagelokal(it)
            } ?: Toast.makeText(this, "Ambil foto terlebih dahulu!", Toast.LENGTH_SHORT).show()
            if (imagePath == null){
                Toast.makeText(this, "Ambil foto terlebih dahulu!", Toast.LENGTH_SHORT).show()
            }else{
                simpan()
            }


        }

        val alasanArray = resources.getStringArray(R.array.alasan_absen)

        val adapter = ArrayAdapter(this, R.layout.spiner_item, alasanArray)
        adapter.setDropDownViewResource(R.layout.spiner_dropdown)

        val alasan = b.spinnerAlasan.selectedItem.toString().trim()


        b.spinnerAlasan.adapter = adapter

        b.spinnerAlasan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                Toast.makeText(this@pengajuanAbsen, "Alasan: $selectedItem", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    }


    private fun simpan() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User belum login!", Toast.LENGTH_SHORT).show()
            return
        }

        val nama = b.EditTextNama.text.toString().trim()
        val alasan = b.spinnerAlasan.selectedItem.toString().trim()


        val tanggal = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val jam = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val izinData = hashMapOf(
            "nama" to nama,
            "alasan" to alasan,
            "tanggal_pengajuan" to tanggal,
            "jam" to jam,
            "foto_path" to imagePath
        )

        firestore.collection("users").document(userId)
            .collection("pengajuanAbsen")
            .add(izinData)
            .addOnSuccessListener {
                Toast.makeText(this, "Izin berhasil diajukan!", Toast.LENGTH_SHORT).show()
            }
    }

    private val ambilgambar = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            Image = imageBitmap
            b.klickKamera.visibility = View.GONE
            b.imageView.visibility = View.VISIBLE
            b.imageView.setImageBitmap(imageBitmap)
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        ambilgambar.launch(intent)
    }

    private fun imagelokal(bitmap: Bitmap): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.jpg"

        val directory = File(filesDir, "PengajuanAbsenImages")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, fileName)

        return try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
            }
            imagePath = file.absolutePath
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
            ""
        }
    }



}