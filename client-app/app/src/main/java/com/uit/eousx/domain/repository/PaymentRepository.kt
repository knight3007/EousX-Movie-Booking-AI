package com.uit.eousx.domain.repository

import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.Payment
import com.uit.eousx.domain.model.SePayPayment
import com.uit.eousx.domain.model.SePayPaymentStatus

interface PaymentRepository {
    suspend fun payMock(bookingId: String): NetworkResult<Payment>
    suspend fun createSePayPayment(bookingId: String): NetworkResult<SePayPayment>
    suspend fun getSePayPaymentStatus(paymentCode: String): NetworkResult<SePayPaymentStatus>
}
