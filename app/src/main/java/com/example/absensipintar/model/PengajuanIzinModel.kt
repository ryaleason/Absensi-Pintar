package com.example.absensipintar.model

class PengajuanIzinModel (
    var documentId: String = "",
    var izinId: String = "",
    var userId: String = "",
    val alasan: String? = null,
    val foto_path: String? = null,
    val nama: String? = null,
    val tanggal_akhir: String? = null,
    val tanggal_awal: String = "",
    val tanggal_pengajuan: String? = null,
    val izinAdmin:String? = null
)
