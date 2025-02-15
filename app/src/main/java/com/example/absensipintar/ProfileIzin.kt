package com.example.absensipintar

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensipintar.AjukanIzinAdmin.AbsenAdapter
import com.example.absensipintar.databinding.ActivityAjukanIzinAdminBinding
import com.example.absensipintar.databinding.FragmentProfileIzinBinding
import com.example.absensipintar.databinding.ItemajukanizinBinding
import com.example.absensipintar.model.PengajuanIzinModel
import com.example.absensipintar.utils.collapseView
import com.example.absensipintar.utils.expandView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProfileIzin : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var absenAdapter: AbsenAdapter
    private val absenList = mutableListOf<PengajuanIzinModel>()
    private lateinit var b: FragmentProfileIzinBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        FirebaseApp.initializeApp(requireContext())
        b = FragmentProfileIzinBinding.inflate(layoutInflater)


        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: ""

        Log.d("USERRID",userId)

        absenAdapter = AbsenAdapter(absenList,db)
        Log.d("MODELLIST",absenList.toString())
        b.ryc.layoutManager = LinearLayoutManager(requireContext())
        b.ryc.adapter = absenAdapter


        loadAbsenData(userId)

        return b.root

    }

    private fun loadAbsenData(userId : String) {
        db.collection("users").document(userId).collection("Izin")
            .get()
            .addOnSuccessListener { documents ->
                absenList.clear()
                if (documents.isEmpty) {

                    b.ryc.visibility = View.GONE
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val absen = document.toObject(PengajuanIzinModel::class.java)
                    absen.izinId = document.id

                    val pathSegments = document.reference.path.split("/")
                    if (pathSegments.size >= 3) {
                        absen.userId = pathSegments[pathSegments.size - 3]
                    } else {
                        absen.userId = ""
                    }



                    absenList.add(absen)
                }

                b.ryc.visibility = View.VISIBLE
                absenAdapter.notifyDataSetChanged()
            }

    }

    class AbsenAdapter(private val absenList: List<PengajuanIzinModel>, private val db: FirebaseFirestore) :
        RecyclerView.Adapter<AbsenAdapter.AbsenViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbsenViewHolder {
            val binding = ItemajukanizinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AbsenViewHolder(binding, db)
        }

        override fun onBindViewHolder(holder: AbsenViewHolder, position: Int) {
            holder.bind(absenList[position])
        }

        override fun getItemCount(): Int = absenList.size

        class AbsenViewHolder(private val b: ItemajukanizinBinding, private val db: FirebaseFirestore) :
            RecyclerView.ViewHolder(b.root) {

            fun bind(absen: PengajuanIzinModel) {

                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val parsedDate = absen.tanggal_pengajuan?.let { dateFormat.parse(it) }

                Log.d("tanggalPengajuan", absen.tanggal_pengajuan.toString())

                parsedDate?.let {
                    val bulanFormat = SimpleDateFormat("MMM", Locale.getDefault())
                    val tanggalFormat = SimpleDateFormat("dd", Locale.getDefault())
                    val hariFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))

                    b.bulan.text = bulanFormat.format(it)
                    b.tanggal.text = tanggalFormat.format(it)
                }
                b.namapendek.text = absen.alasan
               b.formnama.visibility = View.GONE
                b.izinWaktu.text = "${absen.tanggal_awal} sd ${absen.tanggal_akhir}"
                b.tanggalPengajuan.text = "Pengajuan: ${absen.tanggal_pengajuan}"
                b.alasan.text = "Alasan: ${absen.alasan}"


                val filePath = absen.foto_path
                val bitmap = BitmapFactory.decodeFile(filePath)
                b.imageView.setImageBitmap(null)
                b.imageView.setImageBitmap(bitmap)

                b.detailLayout.visibility = View.GONE
                b.klick.setOnClickListener {
                    if (b.detailLayout.visibility == View.GONE) {
                        b.detailLayout.expandView()
                    } else {
                        b.detailLayout.collapseView()
                    }
                }

                cekStatus(absen)


                b.setuju.visibility = View.GONE
                b.tidakSetuju.visibility = View.GONE
            }

            private fun cekStatus(absen: PengajuanIzinModel) {
                val izinRef = db.collection("users")
                    .document(absen.userId)
                    .collection("Izin")
                    .document(absen.izinId)

                izinRef.get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        val status = document.getString("izinAdmin")

                        if (status == null){

                        }

                        b.status.text = "Status: $status"

                        if (status == "Disetujui" || status == "Ditolak") {
                            b.setuju.isEnabled = false
                            b.tidakSetuju.isEnabled = false
                            b.setuju.alpha = 0.5f
                            b.tidakSetuju.alpha = 0.5f
                        } else {
                            b.setuju.isEnabled = true
                            b.tidakSetuju.isEnabled = true
                            b.setuju.alpha = 1.0f
                            b.tidakSetuju.alpha = 1.0f
                        }
                    }
                }
            }

            private fun updateIzin(absen: PengajuanIzinModel, status: String) {
                if (absen.userId.isEmpty() || absen.izinId.isEmpty()) {
                    Log.e("FIREBASE_UPDATE", "User ID atau Izin ID kosong!")
                    return
                }

                val izinRef = db.collection("users")
                    .document(absen.userId)
                    .collection("Izin")
                    .document(absen.izinId)

                izinRef.update("izinAdmin", status)
                    .addOnSuccessListener {
                        Log.d("FIREBASE_UPDATE", "Izin berhasil diperbarui ke $status")
                        b.status.text = "Status: $status"

                        b.setuju.isEnabled = false
                        b.tidakSetuju.isEnabled = false
                        b.setuju.alpha = 0.5f
                        b.tidakSetuju.alpha = 0.5f

                        if (status == "Disetujui") {
                            addIzin(absen)
                        }
                    }

            }

            private fun addIzin(absen: PengajuanIzinModel) {

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                val userAbsenRef = db.collection("users")
                    .document(absen.userId)
                    .collection("absen")
                    .document(absen.tanggal_awal)

                val izinData = hashMapOf(
                    "status" to "Izin",
                    "waktuMasuk" to "07:00:00:10",
                    "waktuKeluar" to "07:00:99",
                    "tanggal_awal" to absen.tanggal_awal,
                    "tanggal" to today,
                    "tanggal_akhir" to absen.tanggal_akhir,
                    "alasan" to absen.alasan
                )

                userAbsenRef.set(izinData)
            }
        }
    }


}