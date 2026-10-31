/*
 * Copyright (c) 2026 NobiFile contributors
 *
 * Opening files and folders in Sam's companion apps: Nobi Terminal
 * (com.nobiterminal) and Nobi Code (com.nobicode.editor). Both actions are
 * best-effort and are only shown when the companion app is installed. No
 * folder-opening contract with either app is assumed: the terminal gets the
 * folder path as an intent extra it may ignore, and the editor gets the file
 * as a content URI. Nothing is claimed to work until Sam tests it on his
 * phone.
 */

package me.zhanghai.android.files.file

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import com.nobifile.manager.R
import java8.nio.file.Path

object NobiIntegration {

    const val NOBI_TERMINAL_PACKAGE = "com.nobiterminal"
    const val NOBI_CODE_PACKAGE = "com.nobicode.editor"

    fun isInstalled(packageManager: PackageManager, packageName: String): Boolean =
        try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    fun launchIntent(
        packageManager: PackageManager,
        packageName: String
    ): Intent? = packageManager.getLaunchIntentForPackage(packageName)

    fun openInNobiTerminal(context: Context, file: FileItem) {
        val directory: Path = if (file.attributes.isDirectory) {
            file.path
        } else {
            file.path.parent ?: return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(NOBI_TERMINAL_PACKAGE)
            ?: run {
                Toast.makeText(
                    context, R.string.nobi_terminal_not_installed, Toast.LENGTH_SHORT
                ).show()
                return
            }
        intent.putExtra("path", directory.toString())
        intent.putExtra("cwd", directory.toString())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context, R.string.nobi_terminal_not_installed, Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun openInNobiCode(context: Context, file: FileItem) {
        if (file.attributes.isDirectory) {
            return
        }
        val uri = try {
            file.path.fileProviderUri
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, R.string.nobi_code_not_installed, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, file.mimeType.value)
            setPackage(NOBI_CODE_PACKAGE)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            Toast.makeText(context, R.string.nobi_code_not_installed, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, R.string.nobi_code_not_installed, Toast.LENGTH_SHORT).show()
        }
    }
}
