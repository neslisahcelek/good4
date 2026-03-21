package com.good4.order.data.repository

import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.order.data.dto.OrderDto
import com.good4.order.data.dto.OrderItemDto
import com.good4.order.domain.Order
import com.good4.order.domain.OrderItem
import com.good4.order.domain.OrderStatus
import kotlinx.datetime.Instant

private const val COLLECTION = "orders"

class OrderRepository(
    private val firestoreRepository: FirestoreRepository
) {
    suspend fun getOrder(id: String): Result<Order, Error> {
        return when (val result = firestoreRepository.getDocument(COLLECTION, id, OrderDto::class)) {
            is Result.Success -> Result.Success(result.data.toOrder(id))
            is Result.Error -> result
        }
    }

    suspend fun getAllOrders(): Result<List<Order>, Error> {
        return when (val result = firestoreRepository.getCollectionWithIds(COLLECTION, OrderDto::class)) {
            is Result.Success -> Result.Success(result.data.map { it.data.toOrder(it.id) })
            is Result.Error -> result
        }
    }

    suspend fun getOrdersBySupporter(supporterId: String): Result<List<Order>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds(
            collectionPath = COLLECTION,
            field = "supporterId",
            value = supporterId,
            clazz = OrderDto::class
        )) {
            is Result.Success -> Result.Success(result.data.map { it.data.toOrder(it.id) })
            is Result.Error -> result
        }
    }

    suspend fun getOrdersByBusiness(businessId: String): Result<List<Order>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds(
            collectionPath = COLLECTION,
            field = "businessId",
            value = businessId,
            clazz = OrderDto::class
        )) {
            is Result.Success -> Result.Success(result.data.map { it.data.toOrder(it.id) })
            is Result.Error -> result
        }
    }

    suspend fun getOrdersByStatus(status: OrderStatus): Result<List<Order>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds(
            collectionPath = COLLECTION,
            field = "status",
            value = status.value,
            clazz = OrderDto::class
        )) {
            is Result.Success -> Result.Success(result.data.map { it.data.toOrder(it.id) })
            is Result.Error -> result
        }
    }

    suspend fun getOrdersByBusinessAndStatus(
        businessId: String,
        status: OrderStatus
    ): Result<List<Order>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithMultipleConditions(
            collectionPath = COLLECTION,
            conditions = mapOf("businessId" to businessId, "status" to status.value),
            clazz = OrderDto::class
        )) {
            is Result.Success -> Result.Success(result.data.map { it.data.toOrder(it.id) })
            is Result.Error -> result
        }
    }

    /**
     * İşletmeye ait siparişleri `createdAt` azalan sırada, sunucu tarafında [limit] ile döndürür.
     * Bileşik indeks: `businessId` (Asc) + `createdAt` (Desc).
     */
    suspend fun getRecentOrdersByBusiness(businessId: String, limit: Long): Result<List<Order>, Error> {
        return when (
            val result = firestoreRepository.queryCollectionWithMultipleConditionsAndLimit(
                collectionPath = COLLECTION,
                conditions = mapOf("businessId" to businessId),
                orderByField = "createdAt",
                descending = true,
                limit = limit,
                clazz = OrderDto::class
            )
        ) {
            is Result.Success -> Result.Success(result.data.map { it.data.toOrder(it.id) })
            is Result.Error -> result
        }
    }

    suspend fun getOrderByCodeAndBusiness(code: String, businessId: String): Result<Order?, Error> {
        return when (val result = firestoreRepository.queryCollectionWithMultipleConditions(
            collectionPath = COLLECTION,
            conditions = mapOf(
                "code" to code,
                "businessId" to businessId,
                "status" to OrderStatus.PENDING.value
            ),
            clazz = OrderDto::class
        )) {
            is Result.Success -> {
                val item = result.data.firstOrNull()
                Result.Success(item?.data?.toOrder(item.id))
            }
            is Result.Error -> result
        }
    }

    suspend fun createOrder(dto: OrderDto): Result<String, Error> {
        return firestoreRepository.addDocument(COLLECTION, dto)
    }

    suspend fun updateOrder(id: String, dto: OrderDto): Result<Unit, Error> {
        return firestoreRepository.updateDocument(COLLECTION, id, dto)
    }

    suspend fun updateOrderStatus(id: String, status: OrderStatus): Result<Unit, Error> {
        return when (val result = firestoreRepository.getDocument(COLLECTION, id, OrderDto::class)) {
            is Result.Success -> {
                val updated = result.data.copy(status = status.value)
                firestoreRepository.updateDocument(COLLECTION, id, updated)
            }
            is Result.Error -> result
        }
    }

    suspend fun deleteOrder(id: String): Result<Unit, Error> {
        return firestoreRepository.deleteDocument(COLLECTION, id)
    }
}

private fun OrderItemDto.toOrderItem(): OrderItem {
    return OrderItem(
        businessId = businessId ?: "",
        businessName = businessName ?: "",
        productId = productId ?: "",
        productName = productName ?: "",
        quantity = quantity ?: 0,
        unitPrice = unitPrice ?: 0.0,
        totalPrice = totalPrice ?: 0.0
    )
}

private fun OrderDto.toOrder(id: String): Order {
    return Order(
        id = id,
        businessId = businessId ?: "",
        businessName = businessName ?: "",
        code = code ?: "",
        createdAt = createdAt?.let { Instant.fromEpochSeconds(it) },
        expiresAt = expiresAt?.let { Instant.fromEpochSeconds(it) },
        grandTotal = grandTotal ?: 0.0,
        totalAmount = totalAmount ?: 0.0,
        platformDonation = platformDonation ?: 0.0,
        status = OrderStatus.fromValue(status),
        supporterId = supporterId ?: "",
        supporterName = supporterName ?: "",
        items = items?.map { it.toOrderItem() } ?: emptyList()
    )
}
