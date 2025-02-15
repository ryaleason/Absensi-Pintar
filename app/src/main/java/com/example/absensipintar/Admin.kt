    package com.example.absensipintar

    import android.os.Bundle
    import android.view.View
    import androidx.activity.enableEdgeToEdge
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.view.ViewCompat
    import androidx.core.view.WindowInsetsCompat
    import androidx.fragment.app.Fragment
    import androidx.viewpager2.adapter.FragmentStateAdapter
    import androidx.viewpager2.widget.ViewPager2
    import com.example.absensipintar.databinding.ActivityAdminBinding

    class Admin : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val b = ActivityAdminBinding.inflate(layoutInflater)
            setContentView(b.root)

            val viewPager2 = b.viewpager
            val adapter = MenuPagerAdapter(this)
            viewPager2.adapter = adapter



            viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateTabUI(position, b)
                }
            })




            b.home.setOnClickListener { viewPager2.currentItem = 0 }
            b.riwayat.setOnClickListener { viewPager2.currentItem = 1 }
            b.ajukanAcara.setOnClickListener {
                viewPager2.currentItem = 2
                b.ajukanAcara.isSelected = true
                b.home.isSelected = false
                b.profile.isSelected = false
               b.imgsetting.setImageResource(R.drawable.settingsbiru)
                b.textsetting.setTextColor(resources.getColor(R.color.birutua))
            }
            b.profile.setOnClickListener { viewPager2.currentItem = 3 }


        }

        private fun updateTabUI(position: Int, b: ActivityAdminBinding) {
            b.imgsetting.setImageResource(R.drawable.settingsabu)
            b.imglist.setImageResource(R.drawable.todoabu)
            b.imghome.setImageResource(R.drawable.homeabu)
            b.imguser.setImageResource(R.drawable.user)

            b.texthome.setTextColor(resources.getColor(R.color.abu))
            b.textlist.setTextColor(resources.getColor(R.color.abu))
            b.textsetting.setTextColor(resources.getColor(R.color.abu))
            b.textuser.setTextColor(resources.getColor(R.color.abu))


            if (position == 0) {
                b.imghome.setImageResource(R.drawable.homebiru)
                b.texthome.setTextColor(resources.getColor(R.color.birutua))
            }

            if (position == 1) {
                b.imglist.setImageResource(R.drawable.todobiru)
                b.textlist.setTextColor(resources.getColor(R.color.birutua))
            }

            if (position == 2) {
                b.imgsetting.setImageResource(R.drawable.settingsbiru)
                b.textsetting.setTextColor(resources.getColor(R.color.birutua))
            }

            if (position == 3) {
                b.imguser.setImageResource(R.drawable.userbiru)
                b.textuser.setTextColor(resources.getColor(R.color.birutua))
            }
        }



        class MenuPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
            override fun getItemCount(): Int {
                return 4
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> HomeAdminFragment()
                    1 -> RiwayatAdminFragment()
                    2 -> settingFragment()
                    3 -> ProfileFragment()
                    else -> HomeAdminFragment()
                }
            }
        }
     }
