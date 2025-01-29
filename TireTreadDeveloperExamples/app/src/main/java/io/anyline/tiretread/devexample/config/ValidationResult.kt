package io.anyline.tiretread.devexample.config

import io.anyline.tiretread.sdk.scanner.MeasurementSystem
import io.anyline.tiretread.sdk.scanner.ScanSpeed
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class ValidationResult() {
    @Serializable
    data class Succeed(
        val validatedContent: SelectConfigContent,
        val configFileContent: String? = null,
        val scanSpeed: ScanSpeed? = null,
        val measurementSystem: MeasurementSystem? = null,
        val tireWidth: Int? = null,
        val showGuidance: Boolean? = null): ValidationResult() {

            override fun toString() = Json.encodeToString(this)

            companion object {
                @JvmStatic
                fun fromString(value: String) = Json.decodeFromString<Succeed>(value)
            }
        }
    @Serializable
    data class Failed(
        val validatedContent: SelectConfigContent,
        val message: String): ValidationResult() {

            override fun toString() = Json.encodeToString(this)

            companion object {
                @JvmStatic
                fun fromString(value: String) = Json.decodeFromString<Failed>(value)
            }
        }
}