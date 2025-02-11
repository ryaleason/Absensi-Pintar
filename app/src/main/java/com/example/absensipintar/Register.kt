package com.example.absensipintar

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.absensipintar.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Register : AppCompatActivity() {
    lateinit var auth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        val username = b.username
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        b.register.setOnClickListener {
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

            if (password.length < 6) {
                b.password.error = "Password Minimal 6 Karakter"
                b.password.requestFocus()
                return@setOnClickListener
            }


            val isAdmin = email == "admin@gmail.com"


            RegisterFirebase(email, password, username.text.toString(), isAdmin)
        }

    }

    private fun RegisterFirebase(email: String, password: String, nama: String, isAdmin: Boolean) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    val userMap = hashMapOf(
                        "nama" to nama,
                        "email" to email,
                        "password" to password,
                        "admin" to isAdmin
                    )

                    if (userId != null) {
                        firestore.collection("users")
                            .document(userId)
                            .set(userMap)

                            .addOnSuccessListener {
                                Toast.makeText(this, "Register Berhasil", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, Login::class.java))
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal Menyimpan Data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Gagal Register Firebase", Toast.LENGTH_SHORT).show()
                }
            }
    }
}