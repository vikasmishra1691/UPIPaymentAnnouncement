package com.example.soundpayapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.soundpayapplication.data.PaymentDatabase
import com.example.soundpayapplication.data.PaymentEntity
import com.example.soundpayapplication.data.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PaymentRepository

    private val _payments = MutableStateFlow<List<PaymentEntity>>(emptyList())
    val payments: StateFlow<List<PaymentEntity>> = _payments.asStateFlow()

    private val _todayTotal = MutableStateFlow(0.0)
    val todayTotal: StateFlow<Double> = _todayTotal.asStateFlow()

    private val _todayCount = MutableStateFlow(0)
    val todayCount: StateFlow<Int> = _todayCount.asStateFlow()

    private val _weekTotal = MutableStateFlow(0.0)
    val weekTotal: StateFlow<Double> = _weekTotal.asStateFlow()

    private val _monthTotal = MutableStateFlow(0.0)
    val monthTotal: StateFlow<Double> = _monthTotal.asStateFlow()

    init {
        val paymentDao = PaymentDatabase.getDatabase(application).paymentDao()
        repository = PaymentRepository(paymentDao)

        viewModelScope.launch {
            repository.allPayments.collect { paymentList ->
                _payments.value = paymentList
            }
        }

        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()

            // Today's start time (midnight)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val todayStart = calendar.timeInMillis

            // Week start (7 days ago)
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val weekStart = calendar.timeInMillis

            // Month start (30 days ago)
            calendar.time = Calendar.getInstance().time
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, -29)
            val monthStart = calendar.timeInMillis

            _todayTotal.value = repository.getTotalAmount(todayStart)
            _todayCount.value = repository.getPaymentCount(todayStart)
            _weekTotal.value = repository.getTotalAmount(weekStart)
            _monthTotal.value = repository.getTotalAmount(monthStart)
        }
    }

    fun deleteAllPayments() {
        viewModelScope.launch {
            repository.deleteAll()
            loadStatistics()
        }
    }

    fun deletePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            repository.delete(payment)
            loadStatistics()
        }
    }
}

