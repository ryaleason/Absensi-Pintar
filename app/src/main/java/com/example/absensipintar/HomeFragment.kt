    package com.example.absensipintar

    import android.animation.ObjectAnimator
    import android.content.Context
    import android.content.pm.PackageManager
    import android.graphics.Color
    import android.location.Location
    import android.os.Bundle
    import android.os.Handler
    import android.os.Looper
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.view.animation.LinearInterpolator
    import android.widget.TextView
    import android.widget.Toast
    import androidx.biometric.BiometricManager
    import androidx.biometric.BiometricPrompt
    import androidx.core.app.ActivityCompat
    import androidx.core.content.ContextCompat
    import androidx.fragment.app.Fragment
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.example.absensipintar.databinding.FragmentHomeBinding
    import com.google.android.gms.location.FusedLocationProviderClient
    import com.google.android.gms.location.LocationServices
    import com.google.android.gms.maps.CameraUpdateFactory
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
    import com.example.absensipintar.model.AbsenModel
    import com.google.android.gms.maps.model.CircleOptions
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import java.util.Date


    class HomeFragment : Fragment() {
        private var gagalFinger = 0
        private lateinit var mMap: GoogleMap
        private lateinit var lokasipengguna: FusedLocationProviderClient
        private lateinit var b: FragmentHomeBinding
        private val db = FirebaseFirestore.getInstance()
        private val handler = Handler(Looper.getMainLooper())
        private var rotationAnimator: ObjectAnimator? = null
        private lateinit var absenAdapter: AbsenAdapter
        private val absenList = mutableListOf<AbsenModel>()

        private val lokasiAbsen = LatLng(-8.155307, 113.435150)
        private val radiusAbsen = 500.0

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


            absenAdapter = AbsenAdapter(absenList)
            b.ryc.adapter = absenAdapter
            b.ryc.layoutManager = LinearLayoutManager(requireContext())

            b.riwayat.visibility


            b.absenMasuk.setOnClickListener {
               checkLokasi(userId,today,::checkFingerPrint)
            }


            b.absenKeluar.setOnClickListener {
                checkLokasi(userId,today,::checkFingerPrintKeluar)


            }

            b.close.setOnClickListener {
                b.tidakterlambat.visibility = View.GONE
            }

            b.close2.setOnClickListener {
                b.tidakterlambat.visibility = View.GONE
            }

            checkExistingAbsen(userId,today)
            cek()
            initMap()
            setupSwipeToRefresh(userId, today)
            loadAbsenData()
            return b.root
        }

        private fun checkLokasi(userId: String, today: String, callback: (String, String) -> Unit) {
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
                    val jarak = hitungJarak(
                        location.latitude, location.longitude,
                        lokasiAbsen.latitude, lokasiAbsen.longitude
                    )

                    if (jarak <= radiusAbsen) {
                        callback(userId, today)
                    } else {
                        Toast.makeText(requireContext(), "Anda berada di luar area absen", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal mendapatkan lokasi", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun setupSwipeToRefresh(userId: String, today: String) {
            b.refres.setOnRefreshListener {
                checkExistingAbsen(userId, today)
                izinLokasi()
                b.refres.isRefreshing = false
                checkExistingAbsen(userId,today)
                loadAbsenData()
                cek()
                initMap()
            }
        }


        private fun checkExistingAbsen(nama: String, today: String) {
            db.collection("users").document(nama).collection("absen").document(today)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val waktuMasuk = document.getString("waktuMasuk") ?: ""
                        val waktuKeluar = document.getString("waktuKeluar") ?: ""
                        val tanggalAbsen = document.getString("tanggal") ?: today

                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                            Date()
                        )

                        if (tanggalAbsen != currentDate) {
                            resetAbsen()
                            return@addOnSuccessListener
                        }

                        if (waktuMasuk.isNotEmpty()) {
                            b.absenMasuk.visibility = View.GONE
                            b.textAbsen.visibility = View.VISIBLE
                            b.textAbsen.text = waktuMasuk
                            b.segeraAbsen.visibility = View.GONE

                            val batasWaktu = "08:00:00"
                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            val waktuAbsen = sdf.parse(waktuMasuk)
                            val waktuBatas = sdf.parse(batasWaktu)

                            if (waktuAbsen.after(waktuBatas)) {
                                b.terlambat.visibility = View.VISIBLE
                                b.tidakterlambat.visibility = View.GONE
                            } else {
                                b.terlambat.visibility = View.GONE
                                b.tidakterlambat.visibility = View.VISIBLE
                            }
                        }

                        if (waktuKeluar.isNotEmpty()) {
                            b.absenKeluar.visibility = View.GONE
                            b.absenKeluartext.visibility = View.VISIBLE
                            b.absenKeluartext.text = waktuKeluar
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

                    if (gagalFinger >= 4) {
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

                    if (gagalFinger >= 4) {
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

            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val waktuAbsen = sdf.parse(waktudetik)
            val waktuBatas = sdf.parse(batasWaktu)

            val absenData = hashMapOf(
                "waktuMasuk" to waktudetik,
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
                mMap.addMarker(MarkerOptions().position(lokasiAbsen).title("Lokasi Absen"))

                mMap.addCircle(
                    CircleOptions()
                        .center(lokasiAbsen)
                        .radius(radiusAbsen)
                        .strokeColor(Color.RED)
                        .fillColor(0x2200FF00)
                        .strokeWidth(3f)
                )

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lokasiAbsen, 15f))
                enableMyLocation()
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




    }
