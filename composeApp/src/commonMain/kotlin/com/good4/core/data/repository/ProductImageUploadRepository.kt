package com.good4.core.data.repository

import com.good4.core.domain.Error
import com.good4.core.domain.Result

/**
 * Ürün görseli JPEG baytlarını Firebase Storage'a yükler; indirilebilir HTTPS URL döner.
 * Kaydet akışında (ürün ekle/güncelle) çağrılır; galeri seçiminde anında yükleme yapılmaz.
 */
interface ProductImageUploadRepository {
    suspend fun uploadProductImage(jpegBytes: ByteArray): Result<String, Error>
    suspend fun deleteProductImage(imageUrl: String): Result<Unit, Error>
}
