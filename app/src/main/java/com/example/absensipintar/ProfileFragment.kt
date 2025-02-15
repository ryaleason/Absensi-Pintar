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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.absensipintar.Admin.MenuPagerAdapter
import com.example.absensipintar.databinding.FragmentProfileBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.play.core.integrity.b
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
       val b = FragmentProfileBinding.inflate(layoutInflater)
        b.logout.setOnClickListener {
            startActivity(Intent(requireContext(),PageAwal::class.java))
        }

        val viewPager2 = b.pro
        val adapter = MenuPagerAdapter(requireActivity())
        viewPager2.adapter = adapter

        viewPager2.isUserInputEnabled = true
        b.refres.setOnRefreshListener {
            parentFragmentManager.beginTransaction().detach(this).attach(this).commit()
            b.refres.isRefreshing = false
        }

        return b.root
    }



    class MenuPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int {
            return 3
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> Profile()
                1 -> RiwayatProfile()
                2->ProfileIzin()
                else -> Profile()
            }
        }
    }


}