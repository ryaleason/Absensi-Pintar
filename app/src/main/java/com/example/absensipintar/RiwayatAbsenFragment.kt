package com.example.absensipintar

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        b = FragmentRiwayatAbsenBinding.inflate(layoutInflater)
        absenAdapter = AbsenAdapter(absenList)
        b.ryc.adapter = absenAdapter
        b.ryc.layoutManager = LinearLayoutManager(requireContext())
        loadAbsenData()

        return b.root
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
            holder.terlambatAtauTidak.text = if (waktuMasukDate.after(batasWaktu)) "Terlambat" else "Tepat Waktu"
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
        }
    }


}