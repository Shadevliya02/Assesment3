package com.sharla0139.assesment3.model

data class MenuItem(
    val id: Int,
    val nama: String,
    val deskripsi: String,
    val harga: Long,
    val foto: String,
    val userId: String = ""

)