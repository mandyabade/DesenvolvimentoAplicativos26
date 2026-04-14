package com.ifpr.androidapptemplate.baseclasses

data class Item(
    var endereco: String? = null,
    var nome: String? = null,
    var objetivo: String? = null,
    var tempo: String? = null,
    var frequencia: String? = null,
    val base64Image: String? = null,
    val imageUrl: String? = null
    )

