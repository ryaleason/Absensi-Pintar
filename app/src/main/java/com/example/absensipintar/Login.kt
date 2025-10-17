package com.example.absensipintar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensipintar.database.DatabaseSQLITE
import com.example.absensipintar.databinding.ActivityLoginBinding
import com.example.absensipintar.model.User
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class Login : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private val GOOGLE_SIGN_IN = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        oneTapClient = Identity.getSignInClient(this)

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.gugel_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()


        b.btngoogle.setOnClickListener {
            gogeldaftar()
        }




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

    private fun simpan(uid: String, nama: String?, email: String?) {
        val user = hashMapOf(
            "nama" to (nama ?: "Pengguna"),
            "email" to email,
            "admin" to false
        )

        db.collection("users").document(uid)
            .set(user)
            .addOnSuccessListener {
                loginSukses(nama, email ?: "", false)
            }

    }


    private fun firebasegoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        simpan(user.uid, user.displayName, user.email)
                    }
                } else {
                    Toast.makeText(this, "Autentikasi Google Gagal", Toast.LENGTH_SHORT).show()
                }
            }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN) {
            try {
                val a = oneTapClient.getSignInCredentialFromIntent(data)
                val idToken = a.googleIdToken
                if (idToken != null) {
                    firebasegoogle(idToken)
                }
            } catch (e: Exception) {
            }
        }
    }


    private fun gogeldaftar() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                startIntentSenderForResult(
                    result.pendingIntent.intentSender, GOOGLE_SIGN_IN,
                    null, 0, 0, 0, null
                )
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
                    val dbHelper = DatabaseSQLITE(this)
                    val user = dbHelper.loginUser(email,password)

                    if (user != null && user.password == password) {
                        loginSukses(user.nama, user.email, user.isAdmin)
                    } else {
                        Toast.makeText(this, "Login gagal, email atau password salah", Toast.LENGTH_SHORT).show()
                    }
                }
            }


    }
    private fun loginSukses(nama: String?, email: String, isAdmin: Boolean) {
        getSharedPreferences("DATANAMA", MODE_PRIVATE).edit().putString("NAMA", nama).apply()
        getSharedPreferences("DATAEMAIL", MODE_PRIVATE).edit().putString("EMAIL", email).apply()

        val intent = if (isAdmin) Intent(this, Admin::class.java) else Intent(this, MenuUtama::class.java)
        startActivity(intent)

        Toast.makeText(this, "Selamat Datang $nama", Toast.LENGTH_SHORT).show()
    }




}
