package com.android.mdl.app.util

import android.util.Log
import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.CborException
import co.nstant.`in`.cbor.model.DataItem
import java.io.ByteArrayOutputStream
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPoint
import kotlin.math.min


object FormatUtil {
    // Helper function to convert a byteArray to HEX string
    fun encodeToString(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }

        return sb.toString()
    }


    private const val CHUNK_SIZE = 2048

    /* Debug print */
    fun debugPrint(tag: String, message: String) {
        var i = 0
        while (i < message.length) {
            Log.d(tag, message.substring(i, min(message.length, i + CHUNK_SIZE)))
            i += CHUNK_SIZE
        }
    }

    /* Debug print */
    fun debugPrintEncodeToString(tag: String, bytes: ByteArray) {
        debugPrint(tag, encodeToString(bytes))
    }

    private const val COSE_KEY_KTY = 1
    private const val COSE_KEY_TYPE_EC2 = 2
    private const val COSE_KEY_EC2_CRV = -1
    private const val COSE_KEY_EC2_X = -2
    private const val COSE_KEY_EC2_Y = -3
    private const val COSE_KEY_EC2_CRV_P256 = 1

    fun cborBuildCoseKey(key: PublicKey): DataItem {
        val ecKey: ECPublicKey = key as ECPublicKey
        val w: ECPoint = ecKey.w
        // X and Y are always positive so for interop we remove any leading zeroes
        // inserted by the BigInteger encoder.
        val x = stripLeadingZeroes(w.affineX.toByteArray())
        val y = stripLeadingZeroes(w.affineY.toByteArray())
        return CborBuilder()
            .addMap()
            .put(COSE_KEY_KTY.toLong(), COSE_KEY_TYPE_EC2.toLong())
            .put(COSE_KEY_EC2_CRV.toLong(), COSE_KEY_EC2_CRV_P256.toLong())
            .put(COSE_KEY_EC2_X.toLong(), x)
            .put(COSE_KEY_EC2_Y.toLong(), y)
            .end()
            .build()[0]
    }

    fun cborEncode(dataItem: DataItem): ByteArray {
        val baos = ByteArrayOutputStream()
        try {
            CborEncoder(baos).encode(dataItem)
        } catch (e: CborException) {
            // This should never happen and we don't want cborEncode() to throw since that
            // would complicate all callers. Log it instead.
            throw IllegalStateException("Unexpected failure encoding data", e)
        }
        return baos.toByteArray()
    }

    private fun stripLeadingZeroes(value: ByteArray): ByteArray {
        var n = 0
        while (n < value.size && value[n] == 0x00.toByte()) {
            n++
        }
        return value.copyOfRange(n, value.size)
    }

}