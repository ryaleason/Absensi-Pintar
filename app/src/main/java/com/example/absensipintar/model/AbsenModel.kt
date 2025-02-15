package com.example.absensipintar.model

data class AbsenModel (
        val userId: String = "",
        val tanggal: String = "",
        val waktuMasuk: String = "",
        val waktuKeluar: String = "",
        val tanggal_akhir:String = "",
        val tanggal_awal:String = "",
        var alasan : String? = null
)