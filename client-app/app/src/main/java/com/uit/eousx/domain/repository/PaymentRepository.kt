package com.uit.eousx.domain.repository

import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.Payment

interface PaymentRepository {
    suspend fun payMock(bookingId: String): NetworkResult<Payment>
}
