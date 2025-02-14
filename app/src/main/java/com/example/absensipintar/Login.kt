package com.example.absensipintar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensipintar.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Login : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()


        b.register.setOnClickListener {
            startActivity(Intent(this,Register::class.java))
        }

        b.login.setOnClickListener {
            val email = b.email.text.toString()
            val password = b.password.text.toString()

            if (email.isEmpty()) {
                b.email.error = "Email Harus Diisi"
                b.email.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                b.email.error = "Email Tidak Valid"
                b.email.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                b.password.error = "Password Harus Diisi"
                b.password.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 2) {
                b.password.error = "Password Minimal 3 Karakter"
                b.password.requestFocus()
                return@setOnClickListener
            }

            LoginFirebase(email,password)
        }
    }

    private fun LoginFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    if (userId != null) {
                        db.collection("users").document(userId)
                            .get()

                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val nama = document.getString("nama")
                                    val isAdmin = document.getBoolean("admin") ?: false



                                    if (isAdmin) {
                                        val intent = Intent(this, Admin::class.java)
                                        getSharedPreferences("DATANAMA", MODE_PRIVATE).edit().putString("NAMA",nama).apply()
                                        getSharedPreferences("DATAEMAIL", MODE_PRIVATE).edit().putString("EMAIL",email).apply()
                                        startActivity(intent)
                                        Toast.makeText(
                                            this,
                                            "Selamat Datang $nama",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    } else {
                                        val intent = Intent(this, MenuUtama::class.java)
                                        getSharedPreferences("DATANAMA", MODE_PRIVATE).edit().putString("NAMA",nama).apply()
                                        getSharedPreferences("DATAEMAIL", MODE_PRIVATE).edit().putString("EMAIL",email).apply()
                                        startActivity(intent)
                                        Toast.makeText(
                                            this,
                                            "Selamat Datang $nama",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } else {
                                    Toast.makeText(
                                        this,
                                        "Pengguna tidak ditemukan",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "Login Gagal", Toast.LENGTH_SHORT).show()
                }
            }
    }

}
