package com.example.absensipintar

import android.animation.ObjectAnimator
import android.content.Context
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensipintar.HomeFragment.AbsenAdapter
import com.example.absensipintar.databinding.FragmentHomeAdminBinding
import com.example.absensipintar.databinding.FragmentHomeBinding
import com.example.absensipintar.databinding.ItemhomeadminBinding
import com.example.absensipintar.model.AbsenModel
import com.example.absensipintar.model.AbsenModelAdmin
import com.example.absensipintar.utils.collapseView
import com.example.absensipintar.utils.expandView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeAdminFragment : Fragment() {
    private var gagalFinger = 0
    private lateinit var mMap: GoogleMap
    private lateinit var lokasipengguna: FusedLocationProviderClient
    private lateinit var b: FragmentHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private var rotationAnimator: ObjectAnimator? = null
    private lateinit var absenAdapter: AbsenAdapter
    private val absenList = mutableListOf<AbsenModelAdmin>()
    private val lokasiAbsen = LatLng(-8.155307, 113.435150)
    private val radiusAbsen = 500.0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val b = FragmentHomeAdminBinding.inflate(layoutInflater)
        val nama = requireContext().getSharedPreferences("DATANAMA", Context.MODE_PRIVATE)
            .getString("NAMA", "");
        val today =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        b.nama.text = "Halo $nama"

        absenAdapter = AbsenAdapter(absenList)
        b.ryc.adapter = absenAdapter
        b.ryc.layoutManager = LinearLayoutManager(requireContext())
        tampilabsen(b)
        return b.root
    }

    private fun tampilabsen(b: FragmentHomeAdminBinding) {
        var jumlahTerlambat = 0
        val batasWaktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse("08:00:00")

        db.collection("users").get()
            .addOnSuccessListener { users ->
                for (user in users) {
                    val userId = user.id
                    val username = user.getString("nama") ?: "Tidak Diketahui"
                    val email = user.getString("email") ?: "Tidak Diketahui"



                    db.collection("users").document(userId).collection("absen").get()

                        .addOnSuccessListener { absenDocs ->
                            for (doc in absenDocs) {
                                val absen = doc.toObject(AbsenModelAdmin::class.java)
                                absen.nama = username
                                absen.email = email
                                absenList.add(absen)

                                val waktuMasukDate = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                    .parse(absen.waktuMasuk ?: "")

                                if (waktuMasukDate != null && waktuMasukDate.after(batasWaktu)) {
                                    jumlahTerlambat++
                                }
                            }
                            absenAdapter.notifyDataSetChanged()

                            b.karyawanTerlambat.text = "$jumlahTerlambat"
                        }
                }
            }
    }





    class AbsenAdapter(private val absenList: List<AbsenModelAdmin>) :
        RecyclerView.Adapter<AbsenAdapter.AbsenViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbsenViewHolder {
            val view = ItemhomeadminBinding.inflate(LayoutInflater.from(parent.context),parent,false)
            return AbsenViewHolder(view)
        }

        override fun onBindViewHolder(holder: AbsenViewHolder, position: Int) {
            val absen = absenList[position]


            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = dateFormat.parse(absen.tanggal)
            val bulanFormat = SimpleDateFormat("MMM", Locale.getDefault())
            val hariFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
            val tanggalFormat = SimpleDateFormat("dd", Locale.getDefault())




                holder.posisi.text = "Ada di sekitar kantor"
                holder.posisi.setTextColor(0xFF0FBD0F.toInt())
                holder.posisi.setBackgroundColor(android.graphics.Color.parseColor("#D4FFD4"))



            holder.bulan.text = bulanFormat.format(parsedDate!!)
            holder.tanggal.text = tanggalFormat.format(parsedDate)
            holder.fulldate.text = "${hariFormat.format(parsedDate)}, ${tanggalFormat.format(parsedDate)} ${bulanFormat.format(parsedDate)} ${Calendar.getInstance().get(Calendar.YEAR)}"
            holder.hari.text = absen.nama
            holder.email.text = absen.email
            holder.absenKeluar.text = absen.waktuKeluar ?: "Belum Absen Keluar"
            holder.waktuTiba.text = "Waktu tiba : ${absen.waktuMasuk}"


            val batasWaktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse("08:00:00")
            val waktuMasukDate =
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(absen.waktuMasuk ?: "")

            if (waktuMasukDate.after(batasWaktu)) {
                holder.terlambatAtauTidak.text = "Terlambat"
                holder.terlambatAtauTidak.setTextColor(0xFF960000.toInt())
                holder.status.setBackgroundColor(android.graphics.Color.parseColor("#B80003"))
            } else {
                holder.terlambatAtauTidak.text = "Tepat Waktu"
                holder.terlambatAtauTidak.setTextColor(0xFF049F09.toInt())
                holder.status.setBackgroundColor(android.graphics.Color.parseColor("#1DD600"))
            }

            holder.klick.setOnClickListener {
                if (holder.detailLayout.visibility == View.GONE) {
                    holder.detailLayout.expandView()
                } else {
                    holder.detailLayout.collapseView()
                }

        }
        }

        private fun hitungJarak(start: LatLng, end: LatLng): Double {
            val radiusBumi = 6371000.0
            val dLat = Math.toRadians(end.latitude - start.latitude)
            val dLon = Math.toRadians(end.longitude - start.longitude)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(start.latitude)) * Math.cos(Math.toRadians(end.latitude)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            return radiusBumi * c
        }

        override fun getItemCount(): Int {
            return absenList.size
        }

        class AbsenViewHolder(val b : ItemhomeadminBinding) : RecyclerView.ViewHolder(b.root) {
            val bulan: TextView = b.bulan
            val status : LinearLayout = b.status
            val tanggal: TextView = b.tanggal
            val hari: TextView = b.hari
            val waktuTiba: TextView = b.waktuTiba
            val terlambatAtauTidak: TextView = b.tepatWaktuAtauTidak
            val klick : LinearLayout = b.klick
            val detailLayout : LinearLayout = b.detailLayout
            val absenKeluar : TextView = b.absenKeluar
            val posisi : TextView = b.posisi
            val email = b.email
            val fulldate : TextView = b.fulldate
        }
    }



}
