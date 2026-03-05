package dev.tohure.didblockchainlessdemo.did

import java.math.BigInteger

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun encode(input: ByteArray): String {
        var n = BigInteger(1, input)
        val sb = StringBuilder()
        val base = BigInteger.valueOf(58)
        while (n > BigInteger.ZERO) {
            val (quotient, remainder) = n.divideAndRemainder(base)
            sb.append(ALPHABET[remainder.toInt()])
            n = quotient
        }
        // Ceros iniciales
        input.takeWhile { it == 0.toByte() }.forEach { sb.append(ALPHABET[0]) }
        return sb.reverse().toString()
    }
}