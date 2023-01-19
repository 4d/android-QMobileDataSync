/*
 * Created by qmarciset on 20/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobiledatasync.log

import android.annotation.SuppressLint
import android.content.Context
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

typealias LogElement = Triple<String, Int, String?>

object LogLevelController {
    /**
     *  val VERBOSE = 2
     *  val DEBUG = 3
     *  val INFO = 4
     *  val WARN = 5
     *  val ERROR = 6
     *  val ASSERT = 7
     *  val NONE = 8
     */

    /**
     * The Observable which will receive the log messages which are
     * to be written to disk.
     */
    private val logBuffer = PublishSubject.create<LogElement>()

    /**
     * Flush sends a signal which allows the buffer to release its contents downstream.
     */
    private var flush = BehaviorSubject.create<Long>()

    /**
     * Signal that the flush has completed
     */
    private var flushCompleted = BehaviorSubject.create<Long>()

    private const val logFileMaxSizeThreshold = 32 * 1024 // 32 KB
    private lateinit var logsDirPath: String
    var level = -1

    fun initialize(context: Context, level: Int) {
        this.level = level
        logsDirPath = LogFileHelper.getLogsDirectoryFromPathOrFallback(context.filesDir.absolutePath)
        CurrentLogHelper.clearFiles(context.filesDir.absolutePath)
        Timber.plant(
            CustomDebugTree(),
            FileTree()
        )
    }

    open class CustomDebugTree : Timber.DebugTree() {

        override fun createStackElementTag(element: StackTraceElement): String {
            return String.format(
                locale = Locale.getDefault(),
                format = "Class:%s: Line: %s, Method: %s",
                super.createStackElementTag(element),
                element.lineNumber,
                element.methodName
            )
        }

        override fun isLoggable(tag: String?, priority: Int): Boolean {
            return level <= priority
        }
    }

    /**
     * The FileTree is the additional log handler which we plant.
     * It's role is to buffer logs and periodically write them to disk.
     */
    @SuppressLint("CheckResult")
    class FileTree : CustomDebugTree() {

        companion object {
            private const val INTERVAL_SIGNAL_MINUTES: Long = 5
            private const val PROCESSED_THRESHOLD = 20
        }

        private val logLineTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        init {

            // Maintain a count of the processed LogElements
            var processed = 0
            logBuffer.observeOn(Schedulers.computation())
                // Increment the counter after each item is processed
                // and perform a flush if the criteria is met.
                .doOnEach {
                    processed++

                    if (processed % PROCESSED_THRESHOLD == 0) {
                        flush()
                    }
                }
                // Merge the signal from flush and the signal from
                // the interval observer to create a dual signal.
                .buffer(
                    flush.mergeWith(
                        Observable.interval(
                            INTERVAL_SIGNAL_MINUTES,
                            TimeUnit.MINUTES
                        )
                    )
                )
                .subscribeOn(Schedulers.io())
                .subscribe { logElement ->
                    try {
                        // Open file
                        val file = CurrentLogHelper.findCurrentLogFile(logsDirPath)

                        if (file != null) {

                            // Write to log
                            FileWriter(file, true).use { fw ->
                                // Write log lines to the file
                                var prevDate = ""
                                logElement.forEach { (date, priority, message) ->
                                    if (date != prevDate) {
                                        fw.append("$date\n")
                                        prevDate = date
                                    }
                                    fw.append("[${LogLevel.fromLevel(priority)}] $message\n")
                                }

                                // Write a line indicating the number of log lines proceed
                                /*fw.append(
                                    "${logLineTimeFormat.format(Date())}\t${logLevels[2] *//* Verbose *//*}" +
                                            "\tFlushing logs -- total processed: $processed\n"
                                )*/

                                fw.flush()
                            }

                            // Validate file size
                            flushCompleted.onNext(file.length())
                        }
                    } catch (e: IOException) {
                        Timber.e("An error occurred while writing logs into log file")
                    }
                }

            flushCompleted
                .subscribeOn(Schedulers.io())
                .filter { size -> size > logFileMaxSizeThreshold }
                .subscribe { rotateLogs() }
        }

        /**
         * Schedule this log to be written to disk.
         */
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // For the sake of simplicity we skip logging the exception,
            // but you can parse the exception and and emit it as needed.
            logBuffer.onNext(
                LogElement(
                    logLineTimeFormat.format(
                        Date()
                    ),
                    priority,
                    message
                )
            )
        }
    }

    private fun flush(onComplete: (() -> Unit)? = null) {
        onComplete?.run {
            Timber.w("Subscribing to flush completion handler")

            flushCompleted
                .take(1)
                .timeout(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .onErrorReturn { -1L }
                .filter { it > 0 }
                .subscribe {
                    rotateLogs()

                    // Delegate back to caller
                    onComplete()
                }
        }

        flush.onNext(1L)
    }

    private fun rotateLogs() {
        val file = CurrentLogHelper.findCurrentLogFile(logsDirPath)
        val savedFile = CurrentLogHelper.findSavedLogFile(logsDirPath)

        if (file != null && savedFile != null) {
            // Truncate the file to zero.
            PrintWriter(savedFile).close()

            val byteArray = file.readBytes()
            val whatsBigger = byteArray.size - logFileMaxSizeThreshold
            val sizedByteArray = byteArray.sliceArray(whatsBigger until byteArray.size)

            savedFile.writeBytes(sizedByteArray)

            // Truncate the file to zero.
            PrintWriter(file).close()
        }
    }
}
