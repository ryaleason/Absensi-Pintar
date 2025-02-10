package com.example.absensipintar

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.absensipintar.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private val handler =  Handler(Looper.getMainLooper())
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val b = FragmentHomeBinding.inflate(layoutInflater)
        updateWaktu(b)
        return b.root
    }

    private fun updateWaktu(b : FragmentHomeBinding) {
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
}