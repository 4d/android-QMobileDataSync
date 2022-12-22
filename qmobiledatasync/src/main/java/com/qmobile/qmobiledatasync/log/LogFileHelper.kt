/*
 * Created by qmarciset on 20/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.log

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.qmobile.qmobiledatasync.app.BaseApp
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPOutputStream

@SuppressLint("LogNotTimber")
object LogFileHelper {

    const val sigCrashLogFileName = "sig_crash_log.txt"
    private const val crashLogFilePrefix = "crash_log_"
    private const val logsFolder = "logs"
    private const val zipFileName = "data.zip"
    private const val gZIPBufferSize = 1024
    private val logFileTimeFormat = SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.US)

    fun createLogFile(context: Context, content: String) {
        try {
            val fileName = "$crashLogFilePrefix${getCurrentDateTimeLogFormat()}.txt"
            val newFile = File(getLogsDirectoryFromPathOrFallback(context.filesDir.absolutePath), fileName)
            newFile.apply {
                parentFile?.mkdirs()
                createNewFile()
                writeText(content)
            }
        } catch (ex: FileNotFoundException) {
            Log.e("LogFileHelper", ex.message.orEmpty())
            Log.e("LogFileHelper", "Could not create crash log file")
        } catch (ioe: IOException) {
            Log.e("LogFileHelper", ioe.message.orEmpty())
            Log.e("LogFileHelper", "Could not write to crash log file to log UncaughtException")
        }
    }

    fun getLogsDirectoryFromPathOrFallback(path: String): String {
        // in file explorer, this is data/data/com.qmobile.sample4dapp.files.logs
        val dir = File(path, logsFolder)

        return if (!dir.exists() && !dir.mkdirs()) {
            // Fallback to parent directory
            Log.e("LogFileHelper", "Unable to create logs directory")
            File(path).absolutePath
        } else {
            dir.absolutePath
        }
    }

    fun getCurrentDateTimeLogFormat(): String {
        return logFileTimeFormat.format(Date())
    }

    fun findCrashLogFile(context: Context): File? {
        val path = getLogsDirectoryFromPathOrFallback(context.filesDir.absolutePath)
        return try {
            val file = File(path).walkTopDown().firstOrNull { it.name.startsWith(crashLogFilePrefix) }
                ?: File(path, sigCrashLogFileName)
            if (file.exists()) {
                file
            } else {
                null
            }
        } catch (ex: FileNotFoundException) {
            Log.e("LogFileHelper", ex.message.orEmpty())
            Log.e("LogFileHelper", "Could not find crash log file")
            null
        } catch (ioe: IOException) {
            Log.e("LogFileHelper", ioe.message.orEmpty())
            Log.e("LogFileHelper", "Could not get crash log file")
            null
        }
    }

    fun cleanOlderCrashLogs(context: Context) {
        File(getLogsDirectoryFromPathOrFallback(context.filesDir.absolutePath)).walkTopDown()
            .filter {
                it.name.startsWith(crashLogFilePrefix) ||
                    it.name == zipFileName ||
                    it.name == sigCrashLogFileName
            }
            .forEach { file ->
                file.delete()
            }
        BaseApp.sharedPreferencesHolder.crashLogSavedForLater = ""
    }

    fun compress(file: File): File? {
        return try {
            // Create a new file for the compressed logs.
            val compressed = File(file.parentFile?.absolutePath, zipFileName)
            write(file, compressed)
            compressed
        } catch (e: IOException) {
            Log.e("LogFileHelper", e.message.orEmpty())
            Log.e("LogFileHelper", "An error occurred while compressing log file into gzip format")
            null
        }
    }

    @Suppress("NestedBlockDepth")
    private fun write(file: File, compressed: File) {
        FileInputStream(file).use { fis ->
            FileOutputStream(compressed).use { fos ->
                GZIPOutputStream(fos).use { gzos ->

                    val buffer = ByteArray(gZIPBufferSize)
                    var length = fis.read(buffer)

                    while (length > 0) {
                        gzos.write(buffer, 0, length)
                        length = fis.read(buffer)
                    }

                    // Finish file compressing and close all streams.
                    gzos.finish()
                }
            }
        }
    }
}
