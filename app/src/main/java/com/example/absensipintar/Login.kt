package com.example.absensipintar

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.absensipintar.databinding.ActivityLoginBinding
import com.example.absensipintar.databinding.ActivityPageAwalBinding

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)


        b.login.setOnClickListener {
            startActivity(Intent(this,MenuUtama::class.java))
        }

    }
}