package com.example.simpleexpensesapp

import com.google.firebase.Timestamp

data class Expense(
    val category: String = "",
    val createdAt: Timestamp? = null,
    val date: String = "",
    val net: Double = 0.0,
    val notes: String = "",
    val receipt: FileAttachment? = null,
    val supplier: String = "",
    val vatCode: String = ""
)