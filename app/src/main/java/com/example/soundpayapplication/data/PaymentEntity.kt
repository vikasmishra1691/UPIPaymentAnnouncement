package com.example.soundpayapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: String,
    val senderName: String?,
    val appName: String,
    val timestamp: Long,
    val notificationText: String
)

