package com.example.soundpayapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: PaymentEntity)

    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPayments(limit: Int = 50): Flow<List<PaymentEntity>>

    @Query("SELECT SUM(CAST(REPLACE(amount, '₹', '') AS REAL)) FROM payments WHERE timestamp >= :startTime")
    suspend fun getTotalAmount(startTime: Long): Double?

    @Query("DELETE FROM payments")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(payment: PaymentEntity)

    @Query("SELECT COUNT(*) FROM payments WHERE timestamp >= :startTime")
    suspend fun getPaymentCount(startTime: Long): Int
}

