package io.anyline.tiretread.devexample.advanced

import android.content.Context
import java.io.File
import java.io.IOException

data class MeasurementResultFolder(val folder: File,
                                   val friendlyName: String,
                                   val parent: MeasurementResultFolder?) {

    private val folderList = mutableListOf<MeasurementResultFolder>()
    private val fileList = mutableListOf<MeasurementResultFile>()

    private val allFiles: List<MeasurementResultFile>
        get() {
            return fileList + folderList.flatMap { it.allFiles }
        }

    enum class ListOptions(val description: String) {
        OriginalStructure("Original file structure"),
        GroupByDescription("Group by description");

        companion object {
            fun asDescriptionList(): Array<String?> {
                val descriptionList = entries.map { it.description }
                return descriptionList.toTypedArray()
            }
        }
    }

    fun asOrderedList(): List<Any> {
        return mutableListOf<Any>().apply {
            addAll(folderList)
            addAll(fileList.sortedByDescending { item ->
                item.fileData.measurementResultCustomData.statusHistory.maxByOrNull { it.timestamp }?.timestamp
            })
        }
    }

    companion object {
        private const val RESULTS_FOLDER_NAME = "results"

        @Throws(IOException::class)
        fun getOrCreateResultsFolder(context: Context) : File {
            getResultsFolder(context)?.let {
                return it
            }?: run {
                val newFolder = File(context.filesDir.path + "/" + RESULTS_FOLDER_NAME)
                if (!newFolder.mkdir()) {
                    throw IOException("Unable to create ${newFolder.path}")
                }
                return newFolder
            }
        }

        fun getResultsFolder(context: Context) : File? {
            context.filesDir?.let { filesDir ->
                filesDir.listFiles()?.let { listFiles ->
                    for (file in listFiles) {
                        if (file.isDirectory && file.name == RESULTS_FOLDER_NAME) {
                            return file
                        }
                    }
                }
            }
            return null
        }


        fun loadFromFolder(context: Context,
                           folder: File,
                           friendlyName: String,
                           listOptions: ListOptions
        ): MeasurementResultFolder? {

            val measurementResultsOriginalFolder = loadRecursivelyFromFolder(context,
                folder,
                friendlyName,
                null)

            return when (listOptions) {
                ListOptions.OriginalStructure -> {
                    measurementResultsOriginalFolder
                }
                ListOptions.GroupByDescription -> {
                    MeasurementResultFolder(folder, friendlyName, null).run {
                        measurementResultsOriginalFolder?.allFiles?.forEach { file ->
                            val firstFolder = this.folderList.firstOrNull {
                                it.friendlyName == file.fileData.measurementResultCustomData.description
                            } ?: MeasurementResultFolder(folder, file.fileData.measurementResultCustomData.description?:"", this).also {
                                folderList.add(it)
                            }
                            firstFolder.fileList.add(file)
                        }
                        this
                    }
                }
            }
        }


        private fun loadRecursivelyFromFolder(context: Context,
                                              folder: File,
                                              friendlyName: String,
                                              parent: MeasurementResultFolder?): MeasurementResultFolder? {
            val scanViewConfigFolder = MeasurementResultFolder(folder, friendlyName, parent)
            folder.listFiles()?.also { listFiles ->
                if (listFiles.isNotEmpty()) {
                    listFiles.forEach { fileOrFolder ->
                        if (fileOrFolder.isDirectory) {
                            try {
                                val subFolder = loadRecursivelyFromFolder(context,
                                    fileOrFolder,
                                    fileOrFolder.name,
                                    scanViewConfigFolder)
                                subFolder?.let {
                                    scanViewConfigFolder.folderList.add(it)
                                }
                            }
                            catch (e: IOException) {
                                return null
                            }
                        }
                        else if (fileOrFolder.isFile) {
                            val parentFolder = parent?: scanViewConfigFolder
                            parentFolder.fileList.add(
                                MeasurementResultFile(context, fileOrFolder)
                            )
                        }
                    }
                }
                else {
                    return null
                }
            }
            return scanViewConfigFolder
        }
    }
}