package io.anyline.tiretread.devexample.advanced

import io.anyline.tiretread.devexample.common.MeasurementResultStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class StatusLog(val status: MeasurementResultStatus,
                     val timestamp: @Serializable(with = DateSerializer::class) LocalDateTime
)

@Serializable
data class MeasurementResultCustomData(
    val description: String? = null,
    val position: String? = null,
    val statusHistory: MutableList<StatusLog> = mutableListOf()) {

    fun notifyStatus(newStatus: MeasurementResultStatus) {
        statusHistory.add(StatusLog(newStatus, LocalDateTime.now()))
    }

    override fun toString(): String {
        return Json.encodeToString(this)
    }

    companion object {
        fun fromString(value: String): MeasurementResultCustomData {
            return Json.decodeFromString<MeasurementResultCustomData>(value)
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = LocalDateTime::class)
object DateSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}