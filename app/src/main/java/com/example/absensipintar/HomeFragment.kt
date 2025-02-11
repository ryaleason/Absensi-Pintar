    package com.example.absensipintar

    import android.animation.ObjectAnimator
    import android.content.Context
    import android.content.pm.PackageManager
    import android.location.Location
    import android.os.Bundle
    import android.os.Handler
    import android.os.Looper
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.view.animation.LinearInterpolator
    import android.widget.Toast
    import androidx.biometric.BiometricManager
    import androidx.biometric.BiometricPrompt
    import androidx.core.app.ActivityCompat
    import androidx.core.content.ContextCompat
    import androidx.fragment.app.Fragment
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



    class HomeFragment : Fragment() {
        private lateinit var mMap: GoogleMap
        private lateinit var fusedLocationClient: FusedLocationProviderClient
        private lateinit var b: FragmentHomeBinding
        private val handler = Handler(Looper.getMainLooper())
        private var rotationAnimator: ObjectAnimator? = null

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            b = FragmentHomeBinding.inflate(inflater, container, false)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            updateWaktu(b)

            val nama = requireContext().getSharedPreferences("DATANAMA",Context.MODE_PRIVATE).getString("NAMA","");

            b.name.text = "Halo $nama"


            b.absenMasuk.setOnClickListener {

                checkFingerPrint()

            }



            b.absenKeluar.setOnClickListener {
                checkFingerPrintKeluar()


            }



            cek()
            initMap()


            return b.root
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

        private fun checkFingerPrint() {
            val biometricManager = BiometricManager.from(requireContext())
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {

                    showBiometricPrompt()


                }

                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Toast.makeText(requireContext(), "Tidak dapat digunakan", Toast.LENGTH_SHORT).show()
                }
            }
        }


        private fun checkFingerPrintKeluar() {
            val biometricManager = BiometricManager.from(requireContext())
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {

                    showBiometricPromptKeluar()


                }

                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Toast.makeText(requireContext(), "Tidak dapat digunakan", Toast.LENGTH_SHORT).show()
                }
            }
        }




        private fun showBiometricPrompt() {
            val executor: Executor = ContextCompat.getMainExecutor(requireContext())
            Log.d("FINGERPRINT", "Menampilkan dialog biometric")

            val finger = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(requireContext(), "Absen masuk berhasil!", Toast.LENGTH_SHORT).show()
                    saveAbsenMasuk()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(requireContext(), "Sidik jari tidak dikenali", Toast.LENGTH_SHORT).show()
                }
            })

            val informasi = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Absen Masuk")
                .setSubtitle("Gunakan sidik jari untuk absen")
                .setNegativeButtonText("Batal")
                .build()

            finger.authenticate(informasi)
        }



        private fun showBiometricPromptKeluar() {
            val executor: Executor = ContextCompat.getMainExecutor(requireContext())

            val finger = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    Toast.makeText(requireContext(), "Absen Keluar berhasil!", Toast.LENGTH_SHORT).show()
                    saveAbsenKeluar()
                }


                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(requireContext(), "Kamu membatalkan absen", Toast.LENGTH_SHORT).show()
                }
            })

            val informasi = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Absen Keluar")
                .setSubtitle("Gunakan sidik jari untuk absen")
                .setNegativeButtonText("Batal")
                .build()

            finger.authenticate(informasi)
        }




        private fun saveAbsenMasuk() {
            val waktudetik = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)
            b.absenMasuk.visibility = View.GONE
            b.textAbsen.visibility = View.VISIBLE
            b.textAbsen.text = waktudetik
            b.segeraAbsen.visibility = View.GONE
            b.tidakterlambat.visibility = View.VISIBLE
        }

        private fun saveAbsenKeluar() {
            val waktudetik = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)
            b.absenKeluar.visibility = View.GONE
            b.absenKeluartext.visibility = View.VISIBLE
            b.absenKeluartext.text = waktudetik
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
                izinLokasi()
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
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
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



    }
