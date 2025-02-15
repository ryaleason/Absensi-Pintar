package com.example.absensipintar.model

import com.google.android.gms.maps.model.LatLng


data class AbsenModelAdmin (
    var tanggal: String = "",
    var waktuMasuk: String = "",
    var nama: String = "",
    var email : String? = null,
    var waktuKeluar: String? = null,
    var alasan : String? = null

){
    val smkn6jember = LatLng(-8.155307, 113.435150)
    val rumah = LatLng(-8.217972, 113.379163)
    val smk8jember = LatLng(-8.212767, 113.459315)
}