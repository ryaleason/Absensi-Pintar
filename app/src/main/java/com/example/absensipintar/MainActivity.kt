package com.example.absensipintar

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.absensipintar.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val loading = b.loding
        val animator = ObjectAnimator.ofFloat(loading, "alpha", 0f, 1f)
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
            if (auth.currentUser == null) {
                startActivity(Intent(this, PageAwal::class.java))
                finish()
            }
        }, 3000)
    }

    override fun onStart() {
        super.onStart()
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val nama = document.getString("nama")
                        val isAdmin = document.getBoolean("admin") ?: false

                        getSharedPreferences("DATANAMA", MODE_PRIVATE)
                            .edit()
                            .putString("NAMA", nama)
                            .apply()

                        val intent = if (isAdmin) {
                            Intent(this, Admin::class.java)
                        } else {
                            Intent(this, MenuUtama::class.java)
                        }
                        val progressBar = findViewById<android.view.View>(R.id.progress_bar)
                        val progressContainer = findViewById<android.view.View>(R.id.progress_container)

                        progressContainer.post {
                            val containerWidth = progressContainer.width
                            val animator = ValueAnimator.ofInt(0, containerWidth)
                            animator.duration = 6000
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
                            startActivity(intent)
                        }, 1000)
                    }
                }
        }
    }
}
