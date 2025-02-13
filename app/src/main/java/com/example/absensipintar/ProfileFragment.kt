package com.example.absensipintar

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.absensipintar.databinding.FragmentProfileBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment() {
    private lateinit var mMap: GoogleMap
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       val b = FragmentProfileBinding.inflate(layoutInflater)

        val nama = requireContext().getSharedPreferences("DATANAMA", Context.MODE_PRIVATE).getString("NAMA","");
        val email = requireContext().getSharedPreferences("DATAEMAIL", Context.MODE_PRIVATE).getString("EMAIL","");
        b.nama.text = nama
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: ""
        b.email.text = email


        b.logout.setOnClickListener {
            startActivity(Intent(requireContext(),PageAwal::class.java))
        }
        hitungbsen(userId,b)
        hitungterlambat(userId,b)


        initMap()
        return b.root
    }

    private fun hitungbsen(nama: String ,  b : FragmentProfileBinding) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(nama).collection("absen")
            .get()
            .addOnSuccessListener { documents ->
                val jumlahAbsen = documents.size()
                b.hadir.text = "${jumlahAbsen} Hari"
            }
    }

    private fun hitungterlambat(nama: String , b : FragmentProfileBinding) {
        val db = FirebaseFirestore.getInstance()
        val batasWaktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse("08:00:00")

        db.collection("users").document(nama).collection("absen")
            .get()
            .addOnSuccessListener { documents ->
                var jumlahTerlambat = 0

                for (document in documents) {
                    val waktumasuk = document.getString("waktuMasuk")
                    if (!waktumasuk.isNullOrEmpty()) {
                        val waktuMasukk = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(waktumasuk)
                        if (waktuMasukk != null && waktuMasukk.after(batasWaktu)) {
                            jumlahTerlambat++
                        }
                    }
                }

                b.terlambat.text = "${jumlahTerlambat} Hari"
            }

    }




    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun initMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            mMap = googleMap
            enableMyLocation()
        }
    }
}