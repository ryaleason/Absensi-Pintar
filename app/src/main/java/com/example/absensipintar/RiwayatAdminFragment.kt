package com.example.absensipintar

import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensipintar.RiwayatAbsenFragment.AbsenAdapter
import com.example.absensipintar.databinding.FragmentRiwayatAbsenBinding
import com.example.absensipintar.databinding.FragmentRiwayatAdminBinding
import com.example.absensipintar.databinding.ItemhomeadminBinding
import com.example.absensipintar.model.AbsenModel
import com.example.absensipintar.model.AbsenModelAdmin
import com.example.absensipintar.utils.collapseView
import com.example.absensipintar.utils.expandView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class RiwayatAdminFragment : Fragment() {

    private lateinit var b : FragmentRiwayatAdminBinding
    private val db = FirebaseFirestore.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private var rotationAnimator: ObjectAnimator? = null
    private lateinit var absenAdapter: AbsenAdapter
    private val absenList = mutableListOf<AbsenModelAdmin>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        b = FragmentRiwayatAdminBinding.inflate(layoutInflater)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: ""
        absenAdapter = AbsenAdapter(absenList)
        b.ryc.adapter = absenAdapter
        b.ryc.layoutManager = LinearLayoutManager(requireContext())
        loadAbsenData()
        refres(userId,today)
        b.calender.setOnClickListener {
            pilihtanggal()
        }

        b.exportCSV.setOnClickListener {
            exportToCSV()
        }




            return b.root
    }

    private fun checkPermissionAndExport() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            exportToCSV()
        } else {
            exportToCSV()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            exportToCSV()
        } else {
            Toast.makeText(requireContext(), "Izin diperlukan untuk menyimpan file", Toast.LENGTH_SHORT).show()
        }
    }


    private fun exportToCSV() {
        if (absenList.isEmpty()) {
            Toast.makeText(requireContext(), "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "Riwayat_Absen.csv"
        val csvHeader = "Nama,Email,Tanggal,Waktu Masuk,Waktu Keluar\n"
        val csvData = StringBuilder().apply {
            append(csvHeader)
            for (absen in absenList) {
                append("${absen.nama},${absen.email},${absen.tanggal},${absen.waktuMasuk ?: "-"},${absen.waktuKeluar ?: "-"}\n")
            }
        }

        try {
            val folder = requireContext().getExternalFilesDir(null) ?: requireContext().filesDir
            val file = File(folder, fileName)

            file.writeText(csvData.toString())

            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Bagikan file CSV"))

            Toast.makeText(requireContext(), "File disimpan di: ${file.absolutePath}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal menyimpan file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }




    private fun pilihtanggal() {
        val calendar = Calendar.getInstance()

        val datePickerAwal = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            val tanggalAwal = Calendar.getInstance()
            tanggalAwal.set(year, month, dayOfMonth)

            val datePickerAkhir = DatePickerDialog(requireContext(), { _, year2, month2, dayOfMonth2 ->
                val tanggalAkhir = Calendar.getInstance()
                tanggalAkhir.set(year2, month2, dayOfMonth2)

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val tanggalAwalStr = sdf.format(tanggalAwal.time)
                val tanggalAkhirStr = sdf.format(tanggalAkhir.time)
                b.tanggaLRiwayat.text = "Riwayat: $tanggalAwalStr s.d $tanggalAkhirStr"
                filterabsen(tanggalAwalStr, tanggalAkhirStr)

            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

            datePickerAkhir.show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        datePickerAwal.show()
    }

    private fun filterabsen(tanggalAwal: String, tanggalAkhir: String) {
        absenList.clear()

        db.collection("users").get().addOnSuccessListener { users ->
            val totalUsers = users.size()
            var processedUsers = 0

            for (user in users) {
                val userId = user.id
                val nama = user.getString("nama") ?: "Tidak Diketahui"
                db.collection("users").document(userId).collection("absen")
                    .whereGreaterThanOrEqualTo("tanggal", tanggalAwal)
                    .whereLessThanOrEqualTo("tanggal", tanggalAkhir)
                    .get()
                    .addOnSuccessListener { absenDocs ->
                        for (doc in absenDocs) {
                            val absen = doc.toObject(AbsenModelAdmin::class.java)
                            absen.nama = nama
                            absenList.add(absen)
                        }

                        processedUsers++
                        if (processedUsers == totalUsers) {
                            absenList.sortByDescending { it.tanggal }
                            absenAdapter.notifyDataSetChanged()

                            if (absenList.isEmpty()) {
                                b.ryc.visibility = View.GONE
                                b.riwayat.visibility = View.VISIBLE
                            } else {
                                b.ryc.visibility = View.VISIBLE
                                b.riwayat.visibility = View.GONE
                            }
                        }
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Gagal memfilter data absen", Toast.LENGTH_SHORT).show()
        }
    }



    private fun refres(userId: String, today: String) {
        b.refres.setOnRefreshListener {
            b.refres.isRefreshing = false
            loadAbsenData()
        }
    }

    private fun loadAbsenData() {
        absenList.clear()

        db.collection("users").get().addOnSuccessListener { users ->
            val totalUsers = users.size()
            if (totalUsers == 0) return@addOnSuccessListener

            var processedUsers = 0
            val tempAbsenList = mutableListOf<AbsenModelAdmin>()

            for (user in users) {
                val userId = user.id

                db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
                    val nama = userDoc.getString("nama") ?: "Tidak Diketahui"
                    val email = userDoc.getString("email") ?: "Tidak Diketahui"

                    db.collection("users").document(userId).collection("absen").get()
                        .addOnSuccessListener { absenDocs ->
                            absenDocs.forEach { doc ->
                                val absen = doc.toObject(AbsenModelAdmin::class.java)
                                absen.nama = nama
                                absen.email = email
                                tempAbsenList.add(absen)
                            }

                            processedUsers++

                            // Ketika semua user telah diproses, urutkan dan tampilkan data
                            if (processedUsers == totalUsers) {
                                // Urutkan berdasarkan tanggal terbaru
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                                tempAbsenList.sortWith(compareByDescending<AbsenModelAdmin> { absen ->
                                    try {
                                        if (!absen.tanggal.isNullOrEmpty()) {
                                            dateFormat.parse(absen.tanggal)
                                        } else {
                                            Date(0)
                                        }
                                    } catch (e: Exception) {
                                        Date(0)
                                    }
                                }.thenByDescending { absen ->
                                    try {
                                        if (!absen.waktuMasuk.isNullOrEmpty()) {
                                            timeFormat.parse(absen.waktuMasuk)
                                        } else {
                                            Date(0)
                                        }
                                    } catch (e: Exception) {
                                        Date(0)
                                    }
                                })

                                // Pindahkan data yang sudah diurutkan ke absenList
                                absenList.addAll(tempAbsenList)

                                // Update tampilan
                                if (absenList.isEmpty()) {
                                    b.ryc.visibility = View.GONE
                                    b.riwayat.visibility = View.VISIBLE
                                } else {
                                    b.ryc.visibility = View.VISIBLE
                                    b.riwayat.visibility = View.GONE
                                }
                                absenAdapter.notifyDataSetChanged()
                            }
                        }
                }
            }
        }
    }


    class AbsenAdapter(private val absenList: List<AbsenModelAdmin>) :
        RecyclerView.Adapter<AbsenAdapter.AbsenViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbsenViewHolder {
            val binding = ItemhomeadminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AbsenViewHolder(binding)
        }

        override fun onBindViewHolder(holder: AbsenViewHolder, position: Int) {
            holder.bind(absenList[position])
        }

        override fun getItemCount(): Int = absenList.size

        class AbsenViewHolder(private val b: ItemhomeadminBinding) :
            RecyclerView.ViewHolder(b.root) {

            fun bind(absen: AbsenModelAdmin) {

                if (absen.tanggal.isNullOrEmpty()) {
                    b.bulan.text = "-"
                    b.tanggal.text = "-"
                    b.fulldate.text = "Tanggal tidak tersedia"
                } else {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val parsedDate = try {
                        if (!absen.tanggal.isNullOrEmpty()) {
                            dateFormat.parse(absen.tanggal)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }

                    parsedDate?.let {
                        val bulanFormat = SimpleDateFormat("MMM", Locale.getDefault())
                        val tanggalFormat = SimpleDateFormat("dd", Locale.getDefault())
                        val hariFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))

                        b.bulan.text = bulanFormat.format(it)
                        b.tanggal.text = tanggalFormat.format(it)
                        b.fulldate.text = "${hariFormat.format(it)}, ${tanggalFormat.format(it)} ${
                            bulanFormat.format(it)
                        }"
                    }

                    b.hari.text = absen.nama
                    b.email.text = absen.email
                    b.waktuTiba.text = "Waktu tiba: ${absen.waktuMasuk ?: "Tidak tersedia"}"


                    val batasWaktu =
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse("08:00:00")
                    val waktuMasukDate: Date? = try {
                        if (!absen.waktuMasuk.isNullOrEmpty()) {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(absen.waktuMasuk)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }

                    if (absen.waktuKeluar != null) {
                        b.absenKeluar.text = absen.waktuKeluar
                        b.posisi.text = "Tidak ada di kantor"
                        b.posisi.setBackgroundColor(android.graphics.Color.parseColor("#FFD4D4"))
                        b.posisi.setTextColor(0xFFBD0F0F.toInt())
                    } else {
                        b.absenKeluar.text = "Belum Absen Keluar"
                        b.posisi.text = "Berada di kantor"
                        b.posisi.setBackgroundColor(android.graphics.Color.parseColor("#DCFFD4"))
                        b.posisi.setTextColor(0xFF38BD0F.toInt())
                    }


                    if (absen.alasan != null) {
                        if (absen.alasan == "Macet" || absen.alasan == "Hujan" || absen.alasan == "Ban Bocor" || absen.alasan == "Lainnya"){
                            b.tepatWaktuAtauTidak.text = "Izin Absen"
                            b.tepatWaktuAtauTidak.setTextColor(0xFFB7B7B7.toInt())
                            b.status.setBackgroundColor(android.graphics.Color.parseColor("#B7B7B7"))
                            b.btnPotongGaji.visibility = View.GONE
                        }
                        else {
                            b.formkeluar.visibility = View.GONE
                            b.formstatus.visibility = View.GONE
                            b.tepatWaktuAtauTidak.text = "Izin Acara"
                            b.tepatWaktuAtauTidak.setTextColor(0xFFB7B7B7.toInt())
                            b.status.setBackgroundColor(android.graphics.Color.parseColor("#B7B7B7"))
                            b.btnPotongGaji.visibility = View.GONE
                        }

                    } else {
                        if (waktuMasukDate != null) {

                            if (waktuMasukDate.after(batasWaktu)) {
                                b.tepatWaktuAtauTidak.text = "Terlambat"
                                b.tepatWaktuAtauTidak.setTextColor(0xFF960000.toInt())
                                b.status.setBackgroundColor(android.graphics.Color.parseColor("#B80003"))
                            } else {
                                b.tepatWaktuAtauTidak.text = "Tepat Waktu"
                                b.tepatWaktuAtauTidak.setTextColor(0xFF17AD00.toInt())
                                b.status.setBackgroundColor(android.graphics.Color.parseColor("#17AD00"))
                            }
                        }
                    }

                    b.klick.setOnClickListener {
                        if (b.detailLayout.visibility == View.GONE) {
                            b.detailLayout.expandView()
                        } else {
                            b.detailLayout.collapseView()
                        }
                    }
                }
            }
        }
    }



}