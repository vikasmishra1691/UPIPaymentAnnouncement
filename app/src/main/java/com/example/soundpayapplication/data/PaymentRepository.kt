package com.example.soundpayapplication.data

import kotlinx.coroutines.flow.Flow

class PaymentRepository(private val paymentDao: PaymentDao) {

    val allPayments: Flow<List<PaymentEntity>> = paymentDao.getAllPayments()

    suspend fun insert(payment: PaymentEntity) {
        paymentDao.insert(payment)
    }

    fun getRecentPayments(limit: Int = 50): Flow<List<PaymentEntity>> {
        return paymentDao.getRecentPayments(limit)
    }

    suspend fun getTotalAmount(startTime: Long): Double {
        return paymentDao.getTotalAmount(startTime) ?: 0.0
    }

    suspend fun getPaymentCount(startTime: Long): Int {
        return paymentDao.getPaymentCount(startTime)
    }

    suspend fun deleteAll() {
        paymentDao.deleteAll()
    }

    suspend fun delete(payment: PaymentEntity) {
        paymentDao.delete(payment)
    }
}

