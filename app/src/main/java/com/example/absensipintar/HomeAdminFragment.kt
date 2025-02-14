    package com.example.absensipintar

    import android.animation.ObjectAnimator
    import android.content.Context
    import android.content.Intent
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
    import java.util.Date
    import java.util.Locale

    class HomeAdminFragment : Fragment() {
        private var gagalFinger = 0
        private lateinit var mMap: GoogleMap
        private lateinit var lokasipengguna: FusedLocationProviderClient
        private lateinit var b: FragmentHomeAdminBinding
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
             b = FragmentHomeAdminBinding.inflate(layoutInflater)
            val nama = requireContext().getSharedPreferences("DATANAMA", Context.MODE_PRIVATE)
                .getString("NAMA", "");
            val today =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            b.nama.text = "Halo $nama"

            absenAdapter = AbsenAdapter(absenList)
            b.ryc.adapter = absenAdapter
            b.ryc.layoutManager = LinearLayoutManager(requireContext())
            tampilabsen(b)
            setupSwipeToRefresh(b)

            b.btnIzinAbsen.setOnClickListener {

            }
            b.btnIzinAcara.setOnClickListener {
                startActivity(Intent(requireContext(),AjukanIzinAdmin::class.java))
            }

            return b.root
        }

        private fun setupSwipeToRefresh(b: FragmentHomeAdminBinding) {
            b.refres.setOnRefreshListener {
                tampilabsen(b)
            }
        }

        private fun tampilabsen(b: FragmentHomeAdminBinding) {
            absenList.clear()
            var jumlahTerlambat = 0
            var jumlahPengajuanAbsen = 0
            var jumlahPengajuanIzin = 0
            val batasWaktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse("08:00:00")

            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            db.collection("users").get().addOnSuccessListener { users ->
                val totalUsers = users.size()
                var processedUsers = 0

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

                                if (!absen.tanggal.isNullOrEmpty() && absen.tanggal == todayDate) {
                                    absenList.add(absen)

                                    val waktuMasukDate: Date? = if (!absen.waktuMasuk.isNullOrEmpty()) {
                                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(absen.waktuMasuk)
                                    } else {
                                        null
                                    }

                                    if (waktuMasukDate != null && waktuMasukDate.after(batasWaktu)) {
                                        jumlahTerlambat++
                                    }
                                }
                            }
                            absenAdapter.notifyDataSetChanged()
                            b.karyawanTerlambat.text = "$jumlahTerlambat"
                        }
                }
            }

            db.collectionGroup("pengajuanAbsen").get()
                .addOnSuccessListener { pengajuanDocs ->
                    jumlahPengajuanAbsen = pengajuanDocs.size()
                    b.MengajukanAbsen.text = "$jumlahPengajuanAbsen"
                }

            db.collectionGroup("Izin").get()
                .addOnSuccessListener { pengajuanDocs ->
                    jumlahPengajuanIzin = pengajuanDocs.size()
                    b.ajukanAcara.text = "$jumlahPengajuanIzin"
                }
        }





        class AbsenAdapter(private val absenList: List<AbsenModelAdmin>) :
            RecyclerView.Adapter<AbsenAdapter.AbsenViewHolder>() {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbsenViewHolder {
                val binding =
                    ItemhomeadminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return AbsenViewHolder(binding)
            }

            override fun onBindViewHolder(holder: AbsenViewHolder, position: Int) {
                val absen = absenList[position]
                holder.bind(absen)
            }

            override fun getItemCount(): Int = absenList.size

            inner class AbsenViewHolder( val b: ItemhomeadminBinding) :
                RecyclerView.ViewHolder(b.root) {

                fun bind(absen: AbsenModelAdmin) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val bulanFormat = SimpleDateFormat("MMM", Locale.getDefault())
                    val hariFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
                    val tanggalFormat = SimpleDateFormat("dd", Locale.getDefault())


                    val parsedDate = absen.tanggal?.let { dateFormat.parse(it) }

                    if (parsedDate != null) {
                        b.bulan.text = bulanFormat.format(parsedDate)
                        b.tanggal.text = tanggalFormat.format(parsedDate)
                        b.fulldate.text =
                            "${hariFormat.format(parsedDate)}, ${tanggalFormat.format(parsedDate)} ${
                                bulanFormat.format(parsedDate)
                            } ${Calendar.getInstance().get(Calendar.YEAR)}"
                    } else {
                        b.bulan.text = "N/A"
                        b.tanggal.text = "N/A"
                        b.fulldate.text = "Tanggal tidak tersedia"
                    }

                    b.hari.text = absen.nama
                    b.email.text = absen.email
                    if (absen.waktuKeluar != null){
                        b.absenKeluar.text = absen.waktuKeluar
                        b.posisi.text = "Tidak ada di kantor"
                        b.posisi.setBackgroundColor(android.graphics.Color.parseColor("#FFD4D4"))
                        b.posisi.setTextColor(0xFFBD0F0F.toInt())
                    }else{
                        b.absenKeluar.text = "Belum Absen Keluar"
                        b.posisi.text = "Berada di kantor"
                        b.posisi.setBackgroundColor(android.graphics.Color.parseColor("#DCFFD4"))
                        b.posisi.setTextColor(0xFF38BD0F.toInt())
                    }
                    val batasTidakDiketahui = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse("09:00:00")



                    b.waktuTiba.text = "Waktu tiba: ${absen.waktuMasuk ?: "Tidak tersedia"}"

                    val batasWaktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse("08:00:00")
                    val waktuMasukDate: Date? = if (!absen.waktuMasuk.isNullOrEmpty()) {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(absen.waktuMasuk)
                    } else {
                        null
                    }


                    if (waktuMasukDate != null && batasWaktu != null) {
                        if (waktuMasukDate.after(batasWaktu)) {
                            b.tepatWaktuAtauTidak.text = "Terlambat"
                            b.tepatWaktuAtauTidak.setTextColor(0xFF960000.toInt())
                            b.status.setBackgroundColor(android.graphics.Color.parseColor("#B80003"))
                        }
                        else {
                            b.tepatWaktuAtauTidak.text = "Tepat Waktu"
                            b.tepatWaktuAtauTidak.setTextColor(0xFF049F09.toInt())
                            b.status.setBackgroundColor(android.graphics.Color.parseColor("#1DD600"))
                            b.btnPotongGaji.visibility = View.GONE
                        }
                    } else {
                        b.tepatWaktuAtauTidak.text = "Tidak tersedia"
                        b.tepatWaktuAtauTidak.setTextColor(android.graphics.Color.GRAY)
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
