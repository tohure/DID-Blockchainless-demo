package dev.tohure.didblockchainlessdemo.utils

import java.util.Base64

/**
 * Extension function to encode ByteArray to Base64 URL-safe string without padding.
 * Standardizes Base64 encoding across the app.
 */
fun ByteArray.toBase64Url(): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(this)