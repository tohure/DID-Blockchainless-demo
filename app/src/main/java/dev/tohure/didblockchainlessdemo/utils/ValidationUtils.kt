package dev.tohure.didblockchainlessdemo.utils

import org.json.JSONObject

object ValidationUtils {

    /**
     * Valida si un String es un JSON bien formado.
     *
     * @param jsonString El String a validar.
     * @throws IllegalArgumentException si el String no es un JSON válido.
     */
    fun validateJson(jsonString: String) {
        try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            throw IllegalArgumentException("El texto no es un JSON válido: ${e.message}")
        }
    }
}