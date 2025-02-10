package com.example.absensipintar

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import com.example.absensipintar.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executor

class HomeFragment : Fragment() {

    private lateinit var b: FragmentHomeBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = FragmentHomeBinding.inflate(inflater, container, false)

        updateWaktu(b)

        b.absenMasuk.setOnClickListener {
            checkFingerPrint()
        }

        b.absenKeluar.setOnClickListener {
            checkFingerPrint()
        }


        val rotateAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate)
        b.imgputar.startAnimation(rotateAnimation)

        return b.root
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




    private fun showBiometricPrompt() {
        val executor: Executor = ContextCompat.getMainExecutor(requireContext())

        val finger = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                Toast.makeText(requireContext(), "Absen masuk berhasil!", Toast.LENGTH_SHORT).show()
                saveAbsenMasuk()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)

                Toast.makeText(requireContext(), "$errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(requireContext(), "Kamu membatalkan absen", Toast.LENGTH_SHORT).show()
            }
        })

        val informasi = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Absen Masuk")
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
    }

    private fun saveAbsenKeluar() {
        val waktudetik = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)
        b.absenKeluar.visibility = View.GONE
        b.absenKeluar.visibility = View.VISIBLE
        b.absenKeluartext.text = waktudetik
    }
}
