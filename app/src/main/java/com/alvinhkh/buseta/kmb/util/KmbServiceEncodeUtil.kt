package com.alvinhkh.buseta.kmb.util

import com.alvinhkh.buseta.BuildConfig

import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec

import java.util.Random
import javax.crypto.Cipher

import org.apache.commons.lang3.StringUtils

import timber.log.Timber
import kotlin.math.abs


internal object KmbServiceEncodeUtil {

    private const val AES_CTR_TRANSFORMATION = "AES/CTR/NoPadding"

    private val secretKeySpec = SecretKeySpec(hexToBytes(BuildConfig.KMB_ETA_KEY_SPEC), "AES")

    internal class RequestEncodeParam {
        var ivLong: Long = 0
        var encodedString: String? = null
    }

    fun decodeKmb(s: String, initVector: Long): String {
        try {
            val ivPadded = StringUtils.leftPad(java.lang.Long.toHexString(initVector), 32, "0")
            val instance = Cipher.getInstance(AES_CTR_TRANSFORMATION)
            instance.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(hexToBytes(ivPadded)))
            return String(instance.doFinal(hexToBytes(s)))
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.e("Decode string exception: " + e.message, e)
            return ""
        }

    }

    fun encodeKmb(s: String, initVector: Long?): RequestEncodeParam {
        val requestEncodeParam = RequestEncodeParam()
        try {
            // Create a 50-bit random long
            var ivLong: Long
            if (initVector == null) {
                val rnd = Random()
                ivLong = abs(rnd.nextLong())
                ivLong %= 1L shl 50  // 50 bit
            } else {
                ivLong = initVector
            }
            requestEncodeParam.ivLong = ivLong
            // Pad to 16 byte (128-bit) hex
            val ivPadded = StringUtils.leftPad(java.lang.Long.toHexString(ivLong), 32, "0")
            val instance = Cipher.getInstance(AES_CTR_TRANSFORMATION)
            instance.init(Cipher.ENCRYPT_MODE, secretKeySpec,
                    IvParameterSpec(hexToBytes(ivPadded)))

            requestEncodeParam.encodedString = bytesToHex(instance.doFinal(s.toByteArray()))
        } catch (e: Exception) {
            Timber.e("Encode string exception", e)
        }

        return requestEncodeParam
    }

    private fun hexToBytes(inStr: String): ByteArray {
        var str = inStr
        if (str.length % 2 != 0) {
            str = "0$str"
        }
        val v = ByteArray(str.length / 2)
        for (i in v.indices) {
            val index = i * 2
            val j = Integer.parseInt(str.substring(index, index + 2), 16)
            v[i] = j.toByte()
        }
        return v
    }

    private fun bytesToHex(a: ByteArray): String {
        val sb = StringBuilder(a.size * 2)
        for (b in a)
            sb.append(String.format("%02x", b))
        return sb.toString()
    }
}
