package com.ztc1997.fingerprint2sleep.util

@Suppress("NOTHING_TO_INLINE")
object RC4 {

    fun decry_RC4(data: ByteArray, ky: String): String {
        return asString(RC4Base(data, ky))
    }

    inline fun decry_RC4(data: ByteArray, ky: ByteArray): String {
        return asString(RC4Base(data, ky))
    }

    fun decry_RC4(data: String, ky: String): String {
        return String(RC4Base(HexString2Bytes(data), ky))
    }

    inline fun decry_RC4(data: String, ky: ByteArray): String {
        return String(RC4Base(HexString2Bytes(data), ky))
    }

    fun encry_RC4_byte(data: String, ky: String): ByteArray {
        val b_data = data.toByteArray()
        return RC4Base(b_data, ky)
    }

    fun encry_RC4_byte(data: String, ky: ByteArray): ByteArray {
        val b_data = data.toByteArray()
        return RC4Base(b_data, ky)
    }

    fun encry_RC4_string(data: String, ky: String): String {
        return toHexString(asString(encry_RC4_byte(data, ky)))
    }

    fun encry_RC4_string(data: String, ky: ByteArray): String {
        return toHexString(asString(encry_RC4_byte(data, ky)))
    }

    inline fun asString(buf: ByteArray): String {
        val strbuf = StringBuilder(buf.size)
        for (i in buf.indices) {
            strbuf.append(buf[i].toChar())
        }
        return strbuf.toString()
    }

    inline fun initky(aky: String) = initky(aky.toByteArray())

    inline fun initky(b_ky: ByteArray): ByteArray {
        val state = ByteArray(300)

        for (i in 0..299) {
            state[i] = i.toByte()
        }
        var index1 = 0
        var index2 = 0
        for (i in 0..299) {
            index2 = (b_ky[index1].toInt() and 0xff) + (state[i].toInt() and 0xff) + index2 and 0xff
            val tmp = state[i]
            state[i] = state[index2]
            state[index2] = tmp
            index1 = (index1 + 1) % b_ky.size
        }
        return state
    }

    inline fun toHexString(s: String): String {
        var str = ""
        for (i in 0..s.length - 1) {
            val ch = s[i].toInt()
            var s4 = Integer.toHexString(ch and 0xFF)
            if (s4.length == 1) {
                s4 = '0' + s4
            }
            str += s4
        }
        return str// 0x表示十六进制
    }

    inline fun HexString2Bytes(src: String): ByteArray {
        val size = src.length
        val ret = ByteArray(size / 2)
        val tmp = src.toByteArray()
        for (i in 0..size / 2 - 1) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1])
        }
        return ret
    }

    inline fun uniteBytes(src0: Byte, src1: Byte): Byte {
        var _b0 = java.lang.Byte.decode("0x" + String(byteArrayOf(src0))).toByte().toChar()
        _b0 = (_b0.toInt() shl 4).toChar()
        val _b1 = java.lang.Byte.decode("0x" + String(byteArrayOf(src1))).toByte().toChar()
        val ret = (_b0.toInt() xor _b1.toInt()).toByte()
        return ret
    }

    inline fun RC4Base(input: ByteArray, mKky: String) = RC4Final(input, initky(mKky))
    inline fun RC4Base(input: ByteArray, mKky: ByteArray) = RC4Final(input, initky(mKky))

    inline fun RC4Final(input: ByteArray, ky: ByteArray): ByteArray {
        var x = 0
        var y = 0
        var xorIndex: Int
        val result = ByteArray(input.size)

        for (i in input.indices) {
            x = x + 1 and 0xff
            y = (ky[x].toInt() and 0xff) + y and 0xff
            val tmp = ky[x]
            ky[x] = ky[y]
            ky[y] = tmp
            xorIndex = (ky[x].toInt() and 0xff) + (ky[y].toInt() and 0xff) and 0xff
            result[i] = (input[i].toInt() xor ky[xorIndex].toInt()).toByte()
        }
        return result
    }
}