package io.anyline.tiretread.devexample.advanced

import android.content.Context
import io.anyline.tiretread.devexample.common.MeasurementResultData
import io.anyline.tiretread.devexample.advanced.MeasurementResultFolder.Companion.getOrCreateResultsFolder
import io.anyline.tiretread.devexample.advanced.MeasurementResultFolder.Companion.getResultsFolder
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

class MeasurementResultFile(context: Context,
                            val fileData: FileData
) {

    @Serializable
    data class FileData(
        val measurementResultData: MeasurementResultData,
        val measurementResultCustomData: MeasurementResultCustomData
    ) {

        fun getCaption(): String {
            with (measurementResultCustomData) {
                return "$description ($position)"
            }
        }

        override fun toString(): String {
            return Json.encodeToString(this)
        }

        companion object {
            fun fromString(value: String): FileData {
                return Json.decodeFromString<FileData>(value)
            }
        }
    }

    private val resultsFolder: File = getOrCreateResultsFolder(context)

    internal constructor(context: Context, storedFile: File)
            : this(context, loadFromFile(storedFile))

    fun save() {
        saveToFile(
            getResultFile(resultsFolder, fileData.measurementResultData.measurementUUID),
            fileData.toString().encodeToByteArray())
    }

    private fun saveToFile(file: File, fileContent: ByteArray) {
        file.outputStream().run {
            write(fileContent)
            close()
        }
    }

    companion object {

        private fun getResultFile(folder: File, measurementUUID: String)
        : File = File(folder,"$measurementUUID.json")

        fun deleteResults(context: Context): Boolean {
            getResultsFolder(context)?.let {
                return it.deleteRecursively()
            }
            return true
        }

        @Throws (IOException::class)
        private fun loadFromFile(storedFile: File): FileData {
            if (storedFile.exists()) {
                storedFile.inputStream().run {
                    val fileContent = readBytes()
                    return FileData.fromString(fileContent.decodeToString())
                }
            }
            throw IOException()
        }
    }
}