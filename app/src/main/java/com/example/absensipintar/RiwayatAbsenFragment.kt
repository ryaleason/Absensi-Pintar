package com.example.absensipintar

import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absensipintar.HomeFragment.AbsenAdapter
import com.example.absensipintar.databinding.FragmentRiwayatAbsenBinding
import com.example.absensipintar.model.AbsenModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class RiwayatAbsenFragment : Fragment() {
    private lateinit var b : FragmentRiwayatAbsenBinding
    private val db = FirebaseFirestore.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private var rotationAnimator: ObjectAnimator? = null
    private lateinit var absenAdapter: AbsenAdapter
    private val absenList = mutableListOf<AbsenModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: ""
        b = FragmentRiwayatAbsenBinding.inflate(layoutInflater)
        absenAdapter = AbsenAdapter(absenList)
        b.ryc.adapter = absenAdapter
        b.ryc.layoutManager = LinearLayoutManager(requireContext())
        loadAbsenData()
        refres(userId,today)
        b.calender.setOnClickListener {
            pilihtanggal()
        }

        return b.root
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
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: return

        db.collection("users").document(userId).collection("absen")
            .whereGreaterThanOrEqualTo("tanggal", tanggalAwal)
            .whereLessThanOrEqualTo("tanggal", tanggalAkhir)
            .get()
            .addOnSuccessListener { documents ->
                absenList.clear()
                for (document in documents) {
                    val absen = document.toObject(AbsenModel::class.java)
                    absenList.add(absen)
                }

                if (absenList.isEmpty()) {
                    b.ryc.visibility = View.GONE
                    b.riwayat.visibility = View.VISIBLE
                } else {
                    b.ryc.visibility = View.VISIBLE
                    b.riwayat.visibility = View.GONE
                }

                absenAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
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
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: return

        db.collection("users").document(userId).collection("absen")
            .get()
            .addOnSuccessListener { documents ->
                absenList.clear()
                for (document in documents) {
                    val absen = document.toObject(AbsenModel::class.java)
                    absenList.add(absen)
                }

                if (absenList.isEmpty()) {
                    b.ryc.visibility = View.GONE
                    b.riwayat.visibility = View.VISIBLE
                } else {
                    b.ryc.visibility = View.VISIBLE
                    b.riwayat.visibility = View.GONE
                }

                absenAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal mengambil data absen", Toast.LENGTH_SHORT).show()
            }
    }
    class AbsenAdapter(private val absenList: List<AbsenModel>) : RecyclerView.Adapter<AbsenAdapter.AbsenViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbsenViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.itemhome, parent, false)
            return AbsenViewHolder(view)
        }

        override fun onBindViewHolder(holder: AbsenViewHolder, position: Int) {
            val absen = absenList[position]


            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = dateFormat.parse(absen.tanggal)
            val bulanFormat = SimpleDateFormat("MMM", Locale.getDefault())
            val hariFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
            val tanggalFormat = SimpleDateFormat("dd", Locale.getDefault())



            holder.bulan.text = bulanFormat.format(parsedDate!!)
            holder.tanggal.text = tanggalFormat.format(parsedDate)
            holder.hari.text = hariFormat.format(parsedDate)
            holder.waktuTiba.text = "Waktu tiba : ${absen.waktuMasuk}"
            val batasWaktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse("08:00:00")
            val waktuMasukDate = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(absen.waktuMasuk ?: "")
            if (waktuMasukDate.after(batasWaktu)) {
                holder.terlambatAtauTidak.text = "Terlambat"
                holder.terlambatAtauTidak.setTextColor(0xFF960000.toInt())
                holder.status.setBackgroundColor(android.graphics.Color.parseColor("#B80003"))
            } else {
                holder.terlambatAtauTidak.text = "Tepat Waktu"
                holder.terlambatAtauTidak.setTextColor(0xFF049F09.toInt())
                holder.status.setBackgroundColor(android.graphics.Color.parseColor("#1DD600"))
            }
        }

        override fun getItemCount(): Int {
            return absenList.size
        }

        class AbsenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val bulan: TextView = itemView.findViewById(R.id.bulan)
            val tanggal: TextView = itemView.findViewById(R.id.tanggal)
            val hari: TextView = itemView.findViewById(R.id.hari)
            val waktuTiba: TextView = itemView.findViewById(R.id.waktuTiba)
            val terlambatAtauTidak : TextView = itemView.findViewById(R.id.tepatWaktuAtauTidak)
            val status : LinearLayout = itemView.findViewById(R.id.status)
        }
    }


}