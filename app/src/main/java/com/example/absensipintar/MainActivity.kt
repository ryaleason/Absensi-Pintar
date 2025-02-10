package com.example.absensipintar

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.absensipintar.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        val loading = b.loding
        val animator = ObjectAnimator.ofFloat(loading,"alpha",0f,1f)
        animator.duration = 500
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = ValueAnimator.INFINITE
        animator.start()

        val progressBar = b.progressBar
        val progressContainer = b.progressContainer


        progressContainer.post {
            val containerWidth = progressContainer.width


            val animator = ValueAnimator.ofInt(0, containerWidth)
            animator.duration = 3000
            animator.interpolator = LinearInterpolator()

            animator.addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                val params = progressBar.layoutParams
                params.width = value
                progressBar.layoutParams = params
            }

            animator.start()
        }


        progressBar.postDelayed({
            startActivity(Intent(this, PageAwal::class.java))
            finish()
        }, 3000)


    }
}