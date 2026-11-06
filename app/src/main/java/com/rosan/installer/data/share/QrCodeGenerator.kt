// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.share

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Renders QR payloads as a crisp, unscaled Android bitmap. */
object QrCodeGenerator {
    private const val SIZE = 768
    private const val QUIET_ZONE = 2

    fun encode(payload: String): Bitmap? {
        if (payload.isBlank()) return null
        return runCatching {
            val matrix = QRCodeWriter().encode(
                payload,
                BarcodeFormat.QR_CODE,
                SIZE,
                SIZE,
                mapOf(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN to QUIET_ZONE,
                ),
            )
            val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            for (x in 0 until SIZE) {
                for (y in 0 until SIZE) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        }.getOrNull()
    }
}
