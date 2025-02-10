package com.example.absensipintar

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.absensipintar.databinding.ActivityPageAwalBinding

class PageAwal : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityPageAwalBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.next.setOnClickListener {
            startActivity(Intent(this,Login::class.java))
        }

    }
}