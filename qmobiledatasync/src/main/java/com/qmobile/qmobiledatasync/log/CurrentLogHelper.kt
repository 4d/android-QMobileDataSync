/*
 * Created by qmarciset on 20/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.log

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

object CurrentLogHelper {

    private const val currentLogFileName = "insights.txt"
    private const val savedLogFileName = "insights_save.txt"

    fun getCurrentLogText(context: Context): String {
        var text = ""
        val logsDirPath =
            File(LogFileHelper.getLogsDirectoryFromPathOrFallback(context.filesDir.absolutePath)).absolutePath
        findSavedLogFile(logsDirPath)?.readText()?.let { text = it + "\n" }
        findCurrentLogFile(logsDirPath)?.readText()?.let { text += it }
        return text
    }

    fun findCurrentLogFile(logsDirPath: String): File? {
        return findLogFileFromPath(logsDirPath, currentLogFileName)
    }

    fun findSavedLogFile(logsDirPath: String): File? {
        return findLogFileFromPath(logsDirPath, savedLogFileName)
    }

    fun clearFiles(path: String) {
        clearCurrentLogFile(path)
        clearSavedLogFile(path)
    }

    @SuppressLint("LogNotTimber")
    private fun clearCurrentLogFile(path: String) {
        try {
            File(LogFileHelper.getLogsDirectoryFromPathOrFallback(path), currentLogFileName).apply {
                if (exists()) {
                    delete()
                }
                parentFile?.mkdirs()
                createNewFile()
            }
        } catch (ex: FileNotFoundException) {
            Log.e("CurrentLogHelper", ex.message.orEmpty())
            Log.e("CurrentLogHelper", "Could not create log file $currentLogFileName")
        } catch (ex: IOException) {
            Log.e("CurrentLogHelper", ex.message.orEmpty())
            Log.e("CurrentLogHelper", "Could not delete log file $currentLogFileName")
        }
    }

    @SuppressLint("LogNotTimber")
    private fun clearSavedLogFile(path: String) {
        try {
            File(LogFileHelper.getLogsDirectoryFromPathOrFallback(path), savedLogFileName).apply {
                parentFile?.mkdirs()
                if (exists()) {
                    delete()
                }
                createNewFile()
            }
        } catch (ex: FileNotFoundException) {
            Log.e("CurrentLogHelper", ex.message.orEmpty())
            Log.e("CurrentLogHelper", "Could not create log file $savedLogFileName")
        } catch (ex: IOException) {
            Log.e("CurrentLogHelper", ex.message.orEmpty())
            Log.e("CurrentLogHelper", "Could not delete log file $savedLogFileName")
        }
    }

    private fun findLogFileFromPath(logsDirPath: String, fileName: String): File? {
        return try {
            val file = File(logsDirPath, fileName)
            if (file.exists() && !file.canWrite()) {
                throw IOException("Log file not writable")
            }
            file
        } catch (ex: FileNotFoundException) {
            Timber.e(ex.message.orEmpty())
            Timber.e("Could not find $fileName")
            null
        } catch (ioe: IOException) {
            Timber.e(ioe.message.orEmpty())
            Timber.e("Could not get $fileName")
            null
        }
    }
}
