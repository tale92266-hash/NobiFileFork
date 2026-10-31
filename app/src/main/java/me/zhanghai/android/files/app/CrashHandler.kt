/*
 * Copyright (c) 2026 NobiFile contributors
 *
 * Crash capture: when the app crashes, the stack trace is saved to a file
 * in app-private storage before the system crash dialog appears. On the
 * next launch the report is shown so it can be shared for diagnosis, since
 * there is no other way to see what crashed on the phone.
 */

package me.zhanghai.android.files.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nobifile.manager.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler {

    private const val FILE_NAME = "crash-report.txt"
    private const val DOWNLOAD_FILE_NAME = "NobiFile-crash-report.txt"
    private const val MAX_BYTES = 200 * 1024

    fun install() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeReport(thread, throwable)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeReport(thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val header = "NobiFile crash $timestamp\n" +
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), " +
            "${Build.MANUFACTURER} ${Build.MODEL}\n" +
            "Thread: ${thread.name}\n\n"
        val trace = header + Log.getStackTraceString(throwable)
        val file = File(application.filesDir, FILE_NAME)
        val existing = try {
            if (file.exists()) file.readText() else ""
        } catch (e: Exception) {
            ""
        }
        val combined = existing + "\n---\n" + trace
        val bytes = combined.toByteArray()
        val trimmed = if (bytes.size > MAX_BYTES) {
            bytes.copyOfRange(bytes.size - MAX_BYTES, bytes.size)
        } else {
            bytes
        }
        file.writeBytes(trimmed)
        writeDownloadCopy(trace)
    }

    /**
     * Also saves the report where Sam can reach it with any file manager:
     * the Download folder. Direct file access works when all-files access
     * is granted; otherwise the MediaStore Downloads collection is used,
     * which needs no storage permission for the app's own file.
     */
    private fun writeDownloadCopy(text: String) {
        try {
            val downloads =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloads, DOWNLOAD_FILE_NAME)
            file.parentFile?.mkdirs()
            file.writeText(text)
            if (file.exists() && file.length() > 0) {
                return
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        try {
            writeDownloadCopyViaMediaStore(text)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun writeDownloadCopyViaMediaStore(text: String) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, DOWNLOAD_FILE_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = application.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
        ) ?: return
        application.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(text.toByteArray())
        }
    }

    private fun readReport(): String? {
        val file = File(application.filesDir, FILE_NAME)
        if (!file.exists()) {
            return null
        }
        return try {
            file.readText().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun clearReport() {
        try {
            File(application.filesDir, FILE_NAME).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun maybeShow(activity: AppCompatActivity) {
        val report = readReport() ?: return
        val textView = TextView(activity).apply {
            text = activity.getString(R.string.crash_report_intro) + "\n\n" + report
            setTextIsSelectable(true)
            movementMethod = ScrollingMovementMethod()
            setPadding(48, 24, 48, 24)
        }
        val scrollView = ScrollView(activity).apply {
            addView(textView)
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.crash_report_title)
            .setView(scrollView)
            .setPositiveButton(R.string.crash_report_share) { _, _ ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                activity.startActivity(
                    Intent.createChooser(intent, activity.getString(R.string.crash_report_share))
                )
            }
            .setNeutralButton(R.string.crash_report_copy) { _, _ ->
                val clipboard =
                    activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("NobiFile crash report", report))
            }
            .setNegativeButton(R.string.crash_report_dismiss) { _, _ ->
                clearReport()
            }
            .show()
    }
}
