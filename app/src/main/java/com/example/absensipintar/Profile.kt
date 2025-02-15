package com.example.absensipintar

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.absensipintar.databinding.FragmentProfile2Binding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class Profile : Fragment() {
    private lateinit var mMap: GoogleMap
    private lateinit var lokasipengguna: FusedLocationProviderClient
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val b = FragmentProfile2Binding.inflate(layoutInflater)

        lokasipengguna = LocationServices.getFusedLocationProviderClient(requireActivity())
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

        val nama = requireContext().getSharedPreferences("DATANAMA", Context.MODE_PRIVATE).getString("NAMA","");
        val email = requireContext().getSharedPreferences("DATAEMAIL", Context.MODE_PRIVATE).getString("EMAIL","");
        b.nama.text = nama
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: ""
        b.email.text = email



        hitungbsen(userId,b)
        hitungterlambat(userId,b)
        hitungjarak(b)
        initMap()
        return b.root
    }


    private fun hitungbsen(nama: String ,  b : FragmentProfile2Binding) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(nama).collection("absen")
            .get()
            .addOnSuccessListener { documents ->
                val jumlahAbsen = documents.size()
                b.hadir.text = "${jumlahAbsen} Hari"
            }
    }


    private fun hitungterlambat(nama: String , b : FragmentProfile2Binding) {
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

    private fun hitungjarak( b : FragmentProfile2Binding){
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Izin lokasi tidak diberikan", Toast.LENGTH_SHORT).show()
            return
        }

        val locationRequest = com.google.android.gms.location.LocationRequest.create()
        locationRequest.priority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY

        lokasipengguna.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    LokasiAbsen { lokasiAbsen, radiusAbsen ->
                        if (lokasiAbsen != null && radiusAbsen != null) {
                            val jarak = hitungJarak(
                                location.latitude, location.longitude,
                                lokasiAbsen.latitude, lokasiAbsen.longitude
                            )

                            val jarakkm = jarak / 1000
                            val jarakasli = String.format("%.2f", jarakkm)

                            if (jarak > radiusAbsen) {
                                b.statustidakdikantor.visibility = View.VISIBLE
                                b.statuskantor.visibility = View.GONE
                            } else {
                                b.statustidakdikantor.visibility = View.GONE
                                b.statuskantor.visibility = View.VISIBLE
                            }
                            Log.d("Jarak", "Jarak: $jarakkm, Radius: $radiusAbsen")

                        } else {
                            Toast.makeText(requireContext(), "Gagal mengambil lokasi absen", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal mendapatkan lokasi pengguna", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal mendapatkan lokasi pengguna", Toast.LENGTH_SHORT).show()
            }


    }

    fun LokasiAbsen(callback: (LatLng?, Double?) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document("U3KAdLt2qOY9k948AOaqFAZGvvf1")
            .collection("lokasi")
            .document("6fOQUBZPrU31eRR5uSKS")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val latitudeStr = document.getString("latitude")
                    val longitudeStr = document.getString("longitude")
                    val radiusStr = document.getString("radius")

                    val latitude = latitudeStr?.toDoubleOrNull()
                    val longitude = longitudeStr?.toDoubleOrNull()
                    val radius = radiusStr?.toDoubleOrNull()

                    if (latitude != null && longitude != null) {
                        val lokasiAbsen = LatLng(latitude, longitude)
                        callback(lokasiAbsen, radius)
                    } else {
                        callback(null, null)
                    }
                } else {
                    callback(null, null)
                }
            }
            .addOnFailureListener {
                callback(null, null)
            }
    }

    private fun hitungJarak(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
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
            mMap.uiSettings.isZoomControlsEnabled = true
            enableMyLocation()
        }
    }

}