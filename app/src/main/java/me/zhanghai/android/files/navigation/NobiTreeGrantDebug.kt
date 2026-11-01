/*
 * Copyright (c) 2026 NobiFile contributors
 *
 * One-shot diagnostic for the NobiCode / Nobi Terminal sidebar grants:
 * appends what the system actually stored (persisted URI permissions,
 * picker results, takePersistablePermission outcomes) to
 * Download/NobiFile-grant-debug.txt so a "picker asks every time" report
 * can be diagnosed from the device instead of guessed at. Best effort
 * only: every failure is swallowed, never shown to the user.
 */

package me.zhanghai.android.files.navigation

import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import me.zhanghai.android.files.app.contentResolver

object NobiTreeGrantDebug {
    private const val FILE_NAME = "NobiFile-grant-debug.txt"
    private const val MAX_SIZE = 200 * 1024L

    fun describeGrants(): String =
        contentResolver.persistedUriPermissions.joinToString(separator = " | ") {
            "${it.uri} read=${it.isReadPermission} write=${it.isWritePermission}"
        }.ifEmpty { "(none)" }

    fun log(event: String) {
        try {
            if (!Environment.isExternalStorageManager()) {
                return
            }
            val file = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ),
                FILE_NAME
            )
            val timestamp = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.US
            ).format(Date())
            val line = "$timestamp $event\n"
            if (file.exists() && file.length() > MAX_SIZE) {
                file.writeText(line)
            } else {
                file.appendText(line)
            }
        } catch (e: Throwable) {
            // Diagnostic only: never disturb the real flow.
        }
    }
}
