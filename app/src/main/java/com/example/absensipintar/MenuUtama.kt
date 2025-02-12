package com.example.absensipintar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.absensipintar.databinding.ActivityMenuUtamaBinding

class MenuUtama : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityMenuUtamaBinding.inflate(layoutInflater)
        setContentView(b.root)

        val viewPager2 = b.viewpager
        val adapter = MenuPagerAdapter(this)
        viewPager2.adapter = adapter









        b.home.setOnClickListener { viewPager2.currentItem = 0 }
        b.riwayat.setOnClickListener { viewPager2.currentItem = 1 }
        b.ajukanAcara.setOnClickListener {
            viewPager2.currentItem = 2
            b.ajukanAcara.isSelected = true
            b.home.isSelected = false
            b.profile.isSelected = false
        }
        b.profile.setOnClickListener { viewPager2.currentItem = 3 }


    }

    class MenuPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int {
            return 4
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> RiwayatAbsenFragment()
                2 -> AjukanIzinFragment()
                3 -> ProfileFragment()
                else -> HomeFragment()
            }
        }
    }
}