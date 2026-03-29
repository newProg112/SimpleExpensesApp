package com.example.simpleexpensesapp

import com.google.firebase.Timestamp

data class MileageClaim(
    val date: String = "",
    val journey: String = "",
    val fromLocation: String = "",
    val toLocation: String = "",
    val miles: Double = 0.0,
    val ratePerMile: Double = 0.45,
    val total: Double = 0.0,
    val notes: String = "",
    val attachment: FileAttachment? = null,
    val createdAt: Timestamp? = null
)

data class MileageUi(
    val id: String,
    val mileage: MileageClaim
)