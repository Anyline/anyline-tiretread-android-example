package io.anyline.tiretread.devexample.config

import kotlinx.serialization.Serializable

@Serializable
enum class SelectConfigContent(
    val containsConfigFile: Boolean = false,
    val containsScanSpeed: Boolean = false,
    val containsMeasurementSystem: Boolean = false,
    val containsTireWidth: Boolean = false,
    val containsShowGuidance: Boolean = false
) {
    DefaultConfigContent(),
    DefaultConfigWithTireWidthContent(containsTireWidth = true),
    ManualConfigContent(
        containsScanSpeed = true,
        containsMeasurementSystem = true,
        containsTireWidth = true,
        containsShowGuidance = true),
    JsonConfigContent(
        containsConfigFile = true,
        containsTireWidth = true);

    fun hasContentToValidate(): Boolean {
        return containsConfigFile
                || containsScanSpeed
                || containsMeasurementSystem
                || containsTireWidth
                || containsShowGuidance
    }
}