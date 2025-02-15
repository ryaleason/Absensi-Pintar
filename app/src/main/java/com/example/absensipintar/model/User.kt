package com.example.absensipintar.model

data class User(
    val uid: String,
    val nama: String,
    val email: String,
    val password: String,
    val isAdmin: Boolean
)
