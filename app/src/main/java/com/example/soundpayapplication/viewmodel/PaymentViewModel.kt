package com.example.soundpayapplication.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.soundpayapplication.data.PaymentDatabase
import com.example.soundpayapplication.data.PaymentEntity
import com.example.soundpayapplication.data.PaymentRepository
import com.example.soundpayapplication.util.ISTTimeHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "PaymentViewModel"
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

        Log.d(TAG, "ViewModel initialized - setting up payment observer")

        viewModelScope.launch {
            repository.allPayments.collect { paymentList ->
                Log.d(TAG, "======================================")
                Log.d(TAG, "Payment list updated! Count: ${paymentList.size}")
                _payments.value = paymentList

                // Log recent payments for debugging
                paymentList.take(3).forEach { payment ->
                    Log.d(TAG, "  Recent: ${payment.amount} from ${payment.senderName} at ${payment.timestamp}")
                }

                // Automatically reload statistics when payments change
                Log.d(TAG, "Triggering statistics reload...")
                loadStatistics()
                Log.d(TAG, "======================================")
            }
        }
    }

    fun loadStatistics() {
        viewModelScope.launch {
            Log.d(TAG, "--- Loading Statistics ---")

            // Use IST time helper for all time calculations
            val todayStart = ISTTimeHelper.getTodayStartTimeMillis()
            val weekStart = ISTTimeHelper.getWeekStartTimeMillis()
            val monthStart = ISTTimeHelper.getMonthStartTimeMillis()

            val todayStartIST = ISTTimeHelper.formatTimestamp(todayStart)
            Log.d(TAG, "Today start: $todayStartIST ($todayStart)")

            val newTodayTotal = repository.getTotalAmount(todayStart)
            val newTodayCount = repository.getPaymentCount(todayStart)
            val newWeekTotal = repository.getTotalAmount(weekStart)
            val newMonthTotal = repository.getTotalAmount(monthStart)

            Log.d(TAG, "Statistics fetched from DB:")
            Log.d(TAG, "  Today: ₹$newTodayTotal (Count: $newTodayCount)")
            Log.d(TAG, "  Week: ₹$newWeekTotal")
            Log.d(TAG, "  Month: ₹$newMonthTotal")

            // Update StateFlows - this will trigger UI recomposition
            _todayTotal.value = newTodayTotal
            _todayCount.value = newTodayCount
            _weekTotal.value = newWeekTotal
            _monthTotal.value = newMonthTotal

            Log.d(TAG, "StateFlows updated - UI should recompose")
            Log.d(TAG, "--- Statistics Load Complete ---")
        }
    }

    fun deleteAllPayments() {
        viewModelScope.launch {
            Log.d(TAG, "Deleting all payments")
            repository.deleteAll()
            loadStatistics()
        }
    }

    fun deletePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            Log.d(TAG, "Deleting payment: ${payment.amount} from ${payment.senderName}")
            repository.delete(payment)
            loadStatistics()
        }
    }
}

