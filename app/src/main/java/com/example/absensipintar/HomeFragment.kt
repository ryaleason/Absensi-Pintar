    package com.example.absensipintar

    import android.animation.ObjectAnimator
    import android.app.AlertDialog
    import android.content.Context
    import android.content.Intent
    import android.location.Location
    import android.os.Bundle
    import android.os.Handler
    import android.os.Looper
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.view.animation.LinearInterpolator
    import android.widget.LinearLayout
    import android.widget.TextView
    import android.widget.Toast
    import androidx.biometric.BiometricManager
    import androidx.biometric.BiometricPrompt
    import androidx.core.content.ContextCompat
    import androidx.fragment.app.Fragment
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.example.absensipintar.databinding.FragmentHomeBinding
    import com.google.android.gms.location.FusedLocationProviderClient
    import com.google.android.gms.maps.GoogleMap
    import com.google.android.gms.maps.SupportMapFragment
    import com.google.android.gms.maps.model.LatLng
    import com.google.android.gms.maps.model.MarkerOptions
    import java.text.SimpleDateFormat
    import java.util.Calendar
    import java.util.Locale
    import java.util.concurrent.Executor
    import kotlin.math.atan2
    import kotlin.math.cos
    import kotlin.math.sin
    import kotlin.math.sqrt
    import com.example.absensipintar.R
    import com.example.absensipintar.database.DatabaseSQLITE
    import com.example.absensipintar.model.AbsenModel
    import com.google.android.gms.maps.model.Circle
    import com.google.android.gms.maps.model.CircleOptions
    import com.google.android.gms.maps.model.LatLngBounds
    import com.google.android.gms.maps.model.Marker
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import java.util.Date
    import kotlin.math.abs
    import android.Manifest
    import android.content.pm.PackageManager
    import android.graphics.Color
    import androidx.core.app.ActivityCompat
    import com.google.android.gms.location.LocationServices
    import com.google.android.gms.maps.CameraUpdateFactory
    import com.google.android.gms.maps.model.*


    class HomeFragment : Fragment() {
        private var gagalFinger = 0
        private var circle: Circle? = null
        private var marker: Marker? = null
        private lateinit var mMap: GoogleMap
        private lateinit var lokasipengguna: FusedLocationProviderClient
        private lateinit var b: FragmentHomeBinding
        private val db = FirebaseFirestore.getInstance()
        private val handler = Handler(Looper.getMainLooper())
        private var rotationAnimator: ObjectAnimator? = null
        private lateinit var absenAdapter: AbsenAdapter
        private val absenList = mutableListOf<AbsenModel>()

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            b = FragmentHomeBinding.inflate(inflater, container, false)
            lokasipengguna = LocationServices.getFusedLocationProviderClient(requireActivity())
            updateWaktu(b)

            val nama = requireContext().getSharedPreferences("DATANAMA",Context.MODE_PRIVATE).getString("NAMA","");
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            b.name.text = "Halo $nama"
            val user = FirebaseAuth.getInstance().currentUser
            val userId = user?.uid ?: ""


            b.close2.setOnClickListener {
                b.terlambat.visibility = View.GONE
            }

            absenAdapter = AbsenAdapter(absenList,db,userId)
            b.ryc.adapter = absenAdapter
            b.ryc.layoutManager = LinearLayoutManager(requireContext())
            b.jamTerlambat
            b.riwayat.visibility

            cekDanTambahkanAbsenOtomatis(userId,today)
            b.absenMasuk.setOnClickListener {
                val calendar = Calendar.getInstance()
                val jamSekarang = calendar.get(Calendar.HOUR_OF_DAY)

                if (jamSekarang in 6..10) {
                    checkLokasi(nama.toString(), userId, today, ::checkFingerPrint)
                } else {
                    Toast.makeText(context, "Absen masuk hanya bisa dilakukan dari jam 06:00 - 9:59", Toast.LENGTH_SHORT).show()
                }
            }

            b.absenKeluar.setOnClickListener {
                val calendar = Calendar.getInstance()
                val jamSekarang = calendar.get(Calendar.HOUR_OF_DAY)

                if (jamSekarang >= 9) {
                    checkLokasi(nama.toString(), userId, today, ::checkFingerPrintKeluar)
                } else {
                    Toast.makeText(context, "Absen keluar hanya bisa dilakukan mulai jam 14:00", Toast.LENGTH_SHORT).show()
                }
            }

            hitungjarak()
            b.close.setOnClickListener {
                b.tidakterlambat.visibility = View.GONE
            }

            b.close2.setOnClickListener {
                b.terlambat.visibility = View.GONE
            }

            checkExistingAbsen(userId,today)
            cek()
            initMap()
            setupSwipeToRefresh(userId, today)
            loadAbsenData()
           hitungjarak()
            return b.root
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




        private fun hitungjarak(){
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(requireContext(), "Izin lokasi tidak diberikan", Toast.LENGTH_SHORT).show()
                return
            }

            lokasipengguna.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    LokasiAbsen { lokasiAbsen, radiusAbsen ->
                        if (lokasiAbsen != null && radiusAbsen != null) {
                            val jarak = hitungJarak(
                                location.latitude, location.longitude,
                                lokasiAbsen.latitude, lokasiAbsen.longitude
                            )
                            val jarakkm = jarak / 1000
                            val jarakasli = String.format("%.2f", jarakkm)
                            val radiusKm = radiusAbsen / 1000.0

                            Log.d("DEBUGJARAK", "Jarak ke lokasi absen: $jarakkm km")

                            if (jarakkm > radiusKm) {
                                b.jarak.text = "Jarak kamu dengan lokasi absen : $jarakasli Km"
                                b.jarak.visibility = View.VISIBLE
                            } else {
                                b.jarak.visibility = View.GONE
                            }
                        } else {
                            Toast.makeText(requireContext(), "Gagal mengambil lokasi absen", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal mendapatkan lokasi pengguna", Toast.LENGTH_SHORT).show()
                }
            }


        }

        private fun checkLokasi(nama: String,userId: String, today: String, callback: (String, String) -> Unit) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(requireContext(), "Izin lokasi tidak diberikan", Toast.LENGTH_SHORT).show()
                return
            }

            lokasipengguna.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    LokasiAbsen { lokasiAbsen, radiusAbsen ->
                        if (lokasiAbsen != null && radiusAbsen != null) {
                            val jarak = hitungJarak(
                                location.latitude, location.longitude,
                                lokasiAbsen.latitude, lokasiAbsen.longitude
                            )

                            val jarakFormatted = String.format("%.2f", jarak)

                            b.jarak.text = "Jarak kamu dengan lokasi absen : $jarakFormatted Km"

                            if (jarak <= radiusAbsen) {
                                callback(userId, today)
                            } else {
                                absendialog(nama)
                            }
                        } else {
                            Toast.makeText(requireContext(), "Gagal mendapatkan lokasi absen", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal mendapatkan lokasi pengguna", Toast.LENGTH_SHORT).show()
                }
            }


        }

        private fun absendialog(nama: String) {
            AlertDialog.Builder(requireContext())
                .setTitle("Pengajuan Absen")
                .setMessage("Anda berada di luar area absen. Apakah Anda ingin mengajukan pengajuan absen?")
                .setPositiveButton("Ya") { _, _ ->

                    val intent = Intent(requireContext(),pengajuanAbsen::class.java)
                    intent.putExtra("NAMA",nama)
                    startActivity(intent)
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }


        private fun setupSwipeToRefresh(userId: String, today: String) {
            b.refres.setOnRefreshListener {
                checkExistingAbsen(userId, today)
                izinLokasi()
                checkExistingAbsen(userId,today)
                loadAbsenData()
                cek()
                hitungjarak()
                initMap()
                b.refres.isRefreshing = false

            }
        }


        private fun checkExistingAbsen(nama: String, today: String) {
            val userRef = db.collection("users").document(nama)

            userRef.collection("lokasi").document("6fOQUBZPrU31eRR5uSKS")
                .get()
                .addOnSuccessListener { lokasiDoc ->
                    val batasAbsen = lokasiDoc.getString("batasMasukAbsen") ?: "08:00:00"

                    userRef.collection("absen").document(today)
                        .get()
                        .addOnSuccessListener { document ->
                            if (!document.exists()) return@addOnSuccessListener

                            val waktuMasuk = document.getString("waktuMasuk").orEmpty()
                            val waktuKeluar = document.getString("waktuKeluar").orEmpty()
                            val tanggalAbsen = document.getString("tanggal") ?: today

                            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                            if (tanggalAbsen != currentDate) {
                                resetAbsen()
                                return@addOnSuccessListener
                            }

                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            val batasWaktu = sdf.parse(batasAbsen)

                            if (waktuMasuk.isNotEmpty()) {
                                b.absenMasuk.visibility = View.GONE
                                b.textAbsen.apply {
                                    visibility = View.VISIBLE
                                    text = waktuMasuk
                                }
                                b.segeraAbsen.visibility = View.GONE

                                val waktuAbsen = sdf.parse(waktuMasuk)

                                if (waktuAbsen?.after(batasWaktu) == true) {
                                    b.terlambat.visibility = View.VISIBLE
                                    b.tidakterlambat.visibility = View.GONE


                                    val selisihMillis = waktuAbsen.time - batasWaktu.time
                                    val selisihJam = selisihMillis / (1000 * 60 * 60)
                                    val selisihMenit = (selisihMillis / (1000 * 60)) % 60

                                    b.jamTerlambat.text = "$selisihJam jam $selisihMenit menit"
                                } else {
                                    b.terlambat.visibility = View.GONE
                                    b.tidakterlambat.visibility = View.VISIBLE
                                }
                            }

                            if (waktuKeluar.isNotEmpty()) {
                                b.absenKeluar.visibility = View.GONE
                                b.absenKeluartext.apply {
                                    visibility = View.VISIBLE
                                    text = waktuKeluar
                                }
                            }
                        }
                }
        }


        private fun resetAbsen() {
            b.absenMasuk.visibility = View.VISIBLE
            b.textAbsen.visibility = View.GONE
            b.terlambat.visibility = View.GONE
            b.tidakterlambat.visibility = View.GONE
            b.absenKeluar.visibility = View.VISIBLE
            b.absenKeluartext.visibility = View.GONE
            b.segeraAbsen.visibility = View.VISIBLE
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

        private fun updateWaktu(b: FragmentHomeBinding) {
            handler.post(object : Runnable {
                override fun run() {

                    val date = Calendar.getInstance()
                    val wktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    b.waktu.text = wktu.format(date.time)

                    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                    b.tanggal.text = dateFormat.format(date.time)
                    handler.postDelayed(this, 1000)
                }
            })
        }

        private fun checkFingerPrint(nama: String, today: String) {
            val biometricManager = BiometricManager.from(requireContext())
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    showBiometricPrompt(nama, today)
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Toast.makeText(requireContext(), "Tidak dapat digunakan", Toast.LENGTH_SHORT).show()
                }
            }
        }


        private fun checkFingerPrintKeluar(nama: String, today: String) {
            val biometricManager = BiometricManager.from(requireContext())
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    showBiometricPromptKeluar(nama, today)
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Toast.makeText(requireContext(), "Tidak dapat digunakan", Toast.LENGTH_SHORT).show()
                }
            }
        }




        private fun showBiometricPrompt(nama: String, today: String) {
            val executor: Executor = ContextCompat.getMainExecutor(requireContext())
            val finger = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    gagalFinger = 0

                    saveAbsenMasuk(nama, today)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)

                    gagalFinger++

                    if (gagalFinger > 3) {
                        Toast.makeText(requireContext(), "Matikan HP Anda lalu absen kembali", Toast.LENGTH_LONG).show()
                        gagalFinger = 0
                    }
                }
            })

            val informasi = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Absen Masuk")
                .setSubtitle("Gunakan sidik jari untuk absen")
                .setNegativeButtonText("Batal")
                .build()

            finger.authenticate(informasi)
        }



        private fun showBiometricPromptKeluar(nama: String, today: String) {
            val executor: Executor = ContextCompat.getMainExecutor(requireContext())
            val finger = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    gagalFinger = 0
                    saveAbsenKeluar(nama, today)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)

                    gagalFinger++

                    if (gagalFinger >= 3) {
                        Toast.makeText(requireContext(), "Matikan HP Anda lalu absen kembali", Toast.LENGTH_LONG).show()
                        gagalFinger = 0
                    }
                }
            })

            val informasi = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Absen Keluar")
                .setSubtitle("Gunakan sidik jari untuk absen")
                .setNegativeButtonText("Batal")
                .build()

            finger.authenticate(informasi)
        }




        private fun saveAbsenMasuk(nama: String, today: String) {
            val waktudetik = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)
            val batasWaktu = "08:00:00"
            val batasAbsenOtomatis = "08:40:00"

            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val waktuAbsen = sdf.parse(waktudetik)
            val waktuBatas = sdf.parse(batasWaktu)
            val waktuBatasOtomatis = sdf.parse(batasAbsenOtomatis)

            val status = if (waktuAbsen.after(waktuBatas)) "Terlambat" else "Tepat Waktu"
            val absenData = hashMapOf(
                "waktuMasuk" to waktudetik,
                "status" to status,
                "tanggal" to today
            )

            db.collection("users").document(nama).collection("absen").document(today)
                .set(absenData)
                .addOnSuccessListener {
                    b.absenMasuk.visibility = View.GONE
                    b.textAbsen.visibility = View.VISIBLE
                    b.textAbsen.text = waktudetik
                    b.segeraAbsen.visibility = View.GONE

                    Toast.makeText(requireContext(), "Absen masuk berhasil!", Toast.LENGTH_SHORT).show()
                }

            if (waktuAbsen.after(waktuBatas)) {
                b.terlambat.visibility = View.VISIBLE
            } else {
                b.tidakterlambat.visibility = View.VISIBLE
            }

            val dbSQLite = DatabaseSQLITE(requireContext())
            dbSQLite.saveAbsen(nama, today, waktudetik, status  )

            if (status == "Terlambat") {
                b.terlambat.visibility = View.VISIBLE
            } else {
                b.tidakterlambat.visibility = View.VISIBLE
            }
        }


        private fun saveAbsenKeluar(nama: String, today: String) {
            val waktudetik = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)

            db.collection("users").document(nama).collection("absen").document(today)
                .update("waktuKeluar", waktudetik)
                .addOnSuccessListener {
                    b.absenKeluar.visibility = View.GONE
                    b.absenKeluartext.visibility = View.VISIBLE
                    b.absenKeluartext.text = waktudetik
                    Toast.makeText(requireContext(), "Absen keluar berhasil!", Toast.LENGTH_SHORT).show()

                    val dbSQLite = DatabaseSQLITE(requireContext())
                    val success = dbSQLite.saveKeluar(nama, today, waktudetik)
                    if (!success) {
                        Toast.makeText(requireContext(), "Gagal menyimpan ke SQLite", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Gagal absen keluar!", Toast.LENGTH_SHORT).show()
                }
        }


        override fun onResume() {
            super.onResume()
            mulaiPutar()
        }

        override fun onPause() {
            super.onPause()
            brhntiRotation()
        }


        private fun mulaiPutar() {
            if (rotationAnimator == null) {
                rotationAnimator = ObjectAnimator.ofFloat(b.imgputar, View.ROTATION, 0f, 360f).apply {
                    duration = 10000
                    repeatCount = ObjectAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    start()
                }
            } else {
                rotationAnimator?.start()
            }
        }

        private fun brhntiRotation() {
            rotationAnimator?.pause()
        }


        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            initMap()
        }

        private fun initMap() {
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            mapFragment?.getMapAsync { googleMap ->
                mMap = googleMap

                // Aktifkan lokasi pengguna terlebih dahulu
                enableMyLocation()

                // Mendapatkan lokasi absen
                LokasiAbsen { lokasiAbsen, radiusAbsen ->
                    if (lokasiAbsen != null && radiusAbsen != null) {
                        circle?.remove()
                        marker?.remove()

                        marker = mMap.addMarker(
                            MarkerOptions()
                                .position(lokasiAbsen)
                                .title("Lokasi Absen")
                        )

                        circle = mMap.addCircle(
                            CircleOptions()
                                .center(lokasiAbsen)
                                .radius(radiusAbsen)
                                .strokeColor(Color.RED)
                                .fillColor(0x2200FF00)
                                .strokeWidth(3f)
                        )

                        // Mendapatkan lokasi pengguna saat ini
                        getCurrentLocation { userLocation ->
                            if (userLocation != null) {
                                // Tambahkan marker untuk lokasi pengguna
                                mMap.addMarker(
                                    MarkerOptions()
                                        .position(userLocation)
                                        .title("Lokasi Saya")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                                )

                                // Buat bounds yang mencakup kedua lokasi
                                val boundsBuilder = LatLngBounds.Builder()
                                boundsBuilder.include(lokasiAbsen)
                                boundsBuilder.include(userLocation)

                                // Tambahkan area radius ke dalam bounds
                                val radiusInDegrees = radiusAbsen / 111000f
                                boundsBuilder.include(
                                    LatLng(
                                        lokasiAbsen.latitude - radiusInDegrees,
                                        lokasiAbsen.longitude - radiusInDegrees
                                    )
                                )
                                boundsBuilder.include(
                                    LatLng(
                                        lokasiAbsen.latitude + radiusInDegrees,
                                        lokasiAbsen.longitude + radiusInDegrees
                                    )
                                )

                                // Animasi kamera untuk menampilkan kedua lokasi dengan padding
                                val bounds = boundsBuilder.build()
                                val padding = 150 // padding dalam pixel
                                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                                mMap.animateCamera(cameraUpdate)
                            } else {
                                // Jika tidak bisa mendapatkan lokasi pengguna, hanya zoom ke lokasi absen
                                val radiusInDegrees = radiusAbsen / 111000f
                                val bounds = LatLngBounds(
                                    LatLng(
                                        lokasiAbsen.latitude - radiusInDegrees,
                                        lokasiAbsen.longitude - radiusInDegrees
                                    ),
                                    LatLng(
                                        lokasiAbsen.latitude + radiusInDegrees,
                                        lokasiAbsen.longitude + radiusInDegrees
                                    )
                                )
                                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Gagal mendapatkan lokasi absen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        private fun getCurrentLocation(callback: (LatLng?) -> Unit) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                callback(null)
                return
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        callback(userLatLng)
                    } else {
                        callback(null)
                    }
                }
                .addOnFailureListener {
                    callback(null)
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



        private fun izinLokasi() {
            if (!::mMap.isInitialized) {
                return
            }



            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true
                lokasipengguna.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        mMap.addMarker(MarkerOptions().position(userLatLng).title("Lokasi Anda"))
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    }
                }
            }
        }

        private fun cek() {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                izinLokasi()
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            }
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                izinLokasi()
            } else {
                Toast.makeText(requireContext(), "Izin lokasi diperlukan!", Toast.LENGTH_SHORT).show()
            }
        }


        class AbsenAdapter(private val absenList: List<AbsenModel> , val db: FirebaseFirestore,val userId: String) : RecyclerView.Adapter<AbsenAdapter.AbsenViewHolder>() {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbsenViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.itemhome, parent, false)
                return AbsenViewHolder(view)
            }

            override fun onBindViewHolder(holder: AbsenViewHolder, position: Int) {
                val absen = absenList[position]

                if (!absen.tanggal_awal.isNullOrEmpty() && !absen.tanggal_akhir.isNullOrEmpty()) {
                    CekIzin(userId, absen.tanggal, db, absen.tanggal_awal, absen.tanggal_akhir)
                }

                Log.d("USERRID", userId)

                holder.waktuTiba.text = "Waktu tiba : ${absen.waktuMasuk ?: "-"}"

                if (absen.waktuMasuk == "01:10:10") {
                    holder.terlambatAtauTidak.text = "Izin Acara"
                    holder.terlambatAtauTidak.setTextColor(0xFF676767.toInt())
                    holder.waktuTiba.text = "Izin : ${absen.tanggal_awal ?: "-"} sd ${absen.tanggal_akhir ?: "-"}"
                    holder.status.setBackgroundColor(android.graphics.Color.parseColor("#656565"))
                }

                val parsedDate = absen.tanggal?.let {
                    try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
                    } catch (e: Exception) {
                        Log.e("DateError", "Format tanggal salah: $it")
                        null
                    }
                }

                if (parsedDate != null) {
                    val bulanFormat = SimpleDateFormat("MMM", Locale.getDefault())
                    val hariFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
                    val tanggalFormat = SimpleDateFormat("dd", Locale.getDefault())

                    holder.bulan.text = bulanFormat.format(parsedDate)
                    holder.tanggal.text = tanggalFormat.format(parsedDate)
                    holder.hari.text = hariFormat.format(parsedDate)
                } else {
                    holder.bulan.text = "-"
                    holder.tanggal.text = "-"
                    holder.hari.text = "-"
                }

                val lokasiRef = FirebaseFirestore.getInstance().collection("users")
                    .document("U3KAdLt2qOY9k948AOaqFAZGvvf1").collection("lokasi")
                    .document("6fOQUBZPrU31eRR5uSKS")

                lokasiRef.get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        val batasWaktuData = document.getString("batasMasukAbsen") ?: "08:00:00"
                        val batasWaktu = try {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(batasWaktuData)
                        } catch (e: Exception) {
                            Log.e("ParseError", "Gagal parsing batas waktu: $batasWaktuData")
                            null
                        }

                        val waktuMasukDate = absen.waktuMasuk?.let {
                            try {
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(it)
                            } catch (e: Exception) {
                                Log.e("ParseError", "Gagal parsing waktu masuk: $it")
                                null
                            }
                        }

                        if (waktuMasukDate != null && batasWaktu != null) {
                            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                            val waktuMasukString = sdf.format(waktuMasukDate)

                            Log.d("DEBUG_WAKTU", "Waktu Masuk: $waktuMasukString")

                            val batasAwalSecond = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).parse("01:10:09.00")
                            val batasAkhirSecond = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).parse("01:10:12.00")

                            if (absen.alasan != null) {
                                if (absen.alasan == "Macet" || absen.alasan == "Hujan" || absen.alasan == "Ban Bocor" || absen.alasan == "Lainnya"){

                                holder.terlambatAtauTidak.text = "Izin Terlambat"
                                holder.terlambatAtauTidak.setTextColor(0xFF676767.toInt())
                                }else{
                                    holder.terlambatAtauTidak.text = "Izin Acara"
                                    holder.terlambatAtauTidak.setTextColor(0xFF676767.toInt())
                                    holder.waktuTiba.text =
                                        "Izin : ${absen.tanggal_awal ?: "-"} sd ${absen.tanggal_akhir ?: "-"}"

                                }

                            } else {
                                when {
                                    waktuMasukDate.after(batasAwalSecond) && waktuMasukDate.before(batasAkhirSecond) -> {
                                        holder.terlambatAtauTidak.text = "Izin Acara"
                                        holder.terlambatAtauTidak.setTextColor(0xFF676767.toInt())

                                        holder.status.setBackgroundColor(android.graphics.Color.parseColor("#656565"))
                                    }

                                    waktuMasukDate.after(batasWaktu) -> {
                                        holder.terlambatAtauTidak.text = "Terlambat"
                                        holder.terlambatAtauTidak.setTextColor(0xFF960000.toInt())
                                        holder.status.setBackgroundColor(android.graphics.Color.parseColor("#B80003"))
                                    }

                                    else -> {
                                        holder.terlambatAtauTidak.text = "Tepat Waktu"
                                        holder.terlambatAtauTidak.setTextColor(0xFF049F09.toInt())
                                        holder.status.setBackgroundColor(android.graphics.Color.parseColor("#1DD600"))
                                    }
                                }
                            }
                        } else {
                            holder.terlambatAtauTidak.text = "Tidak Hadir"
                            holder.terlambatAtauTidak.setTextColor(0xFFFF0000.toInt())
                            holder.status.setBackgroundColor(android.graphics.Color.parseColor("#FF0000"))
                        }
                    }
                }
            }


            private fun CekIzin(userId: String, today: String, db: FirebaseFirestore, tanggalAwal: String, tanggalAkhir: String) {
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

                try {
                    val currentDate = sdf.parse(today)
                    val startTanggal = sdf.parse(tanggalAwal)
                    val endTanggal = sdf.parse(tanggalAkhir)

                    if (currentDate != null && startTanggal != null && endTanggal != null) {
                        Log.d("CekIzin", "Cek izin untuk tanggal: $today, range: $tanggalAwal - $tanggalAkhir")

                        if (currentDate.compareTo(startTanggal) >= 0 && currentDate.compareTo(endTanggal) <= 0) {
                            val absenData = hashMapOf(
                                "waktuMasuk" to "01:10:10",
                                "waktuKeluar" to "01:10:12",
                                "tanggal" to today
                            )

                            val docRef = db.collection("users").document(userId).collection("absen").document(today)
                        } else {
                            Log.d("CekIzin", "Hari ini ($today) tidak dalam rentang izin ($tanggalAwal - $tanggalAkhir)")
                        }
                    } else {
                    }
                } catch (e: Exception) {
                    Log.e("CekIzin", "${e.message}")
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

        private fun cekDanTambahkanAbsenOtomatis(nama: String, today: String) {
            val currentTime = SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            ).format(Calendar.getInstance().time)
            val batasAbsenOtomatis = "12:00:00"

            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val waktuSekarang = sdf.parse(currentTime)
            val waktuBatasOtomatis = sdf.parse(batasAbsenOtomatis)

            if (waktuSekarang.after(waktuBatasOtomatis)) {
                val absenData = hashMapOf(
                    "waktuMasuk" to "",
                    "waktuKeluar" to "",
                    "tanggal" to today
                )

                db.collection("users").document(nama).collection("absen").document(today)
                    .get()
                    .addOnSuccessListener { document ->
                        if (!document.exists()) {
                            db.collection("users").document(nama).collection("absen")
                                .document(today)
                                .set(absenData)
                        }
                    }
            }
        }




    }
