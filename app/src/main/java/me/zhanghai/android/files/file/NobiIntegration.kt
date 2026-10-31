/*
 * Copyright (c) 2026 NobiFile contributors
 *
 * Opening files and folders in Sam's companion apps: Nobi Terminal
 * (com.nobiterminal) and Nobi Code (com.nobicode.editor).
 *
 * Terminal folders open through Nobi Terminal's RUN_COMMAND service
 * (action com.nobiterminal.RUN_COMMAND, workdir + session-action extras),
 * which needs the dangerous com.nobiterminal.permission.RUN_COMMAND
 * permission (declared in the manifest, granted at runtime) AND
 * allow-external-apps=true in the terminal's ~/.termux/termux.properties.
 * The editor gets the file as a content URI. Both actions are only shown
 * when the companion app is installed. Nothing is claimed to work until
 * Sam tests it on his phone.
 */

package me.zhanghai.android.files.file

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.nobifile.manager.R
import java8.nio.file.Path
import me.zhanghai.android.files.provider.linux.isLinuxPath
import me.zhanghai.android.files.util.showToast

object NobiIntegration {

    const val NOBI_TERMINAL_PACKAGE = "com.nobiterminal"
    const val NOBI_CODE_PACKAGE = "com.nobicode.editor"

    const val RUN_COMMAND_PERMISSION = "com.nobiterminal.permission.RUN_COMMAND"
    private const val RUN_COMMAND_ACTION = "com.nobiterminal.RUN_COMMAND"
    private const val RUN_COMMAND_SERVICE_CLASS =
        "com.nobiterminal.app.RunCommandService"
    private const val EXTRA_RUN_COMMAND_WORKDIR = "com.nobiterminal.RUN_COMMAND_WORKDIR"
    private const val EXTRA_RUN_COMMAND_SESSION_ACTION =
        "com.nobiterminal.RUN_COMMAND_SESSION_ACTION"

    /** Session action 0: switch to a new session and open the activity. */
    private const val SESSION_ACTION_NEW_AND_OPEN = "0"

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

    fun hasRunCommandPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, RUN_COMMAND_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Sends a RUN_COMMAND intent opening an interactive shell in [directory].
     * The caller must hold [RUN_COMMAND_PERMISSION]. Returns false when the
     * terminal refuses the intent.
     */
    fun sendRunCommandIntent(context: Context, directory: Path): Boolean {
        val intent = Intent(RUN_COMMAND_ACTION).apply {
            setClassName(NOBI_TERMINAL_PACKAGE, RUN_COMMAND_SERVICE_CLASS)
            putExtra(EXTRA_RUN_COMMAND_WORKDIR, directory.toString())
            putExtra(EXTRA_RUN_COMMAND_SESSION_ACTION, SESSION_ACTION_NEW_AND_OPEN)
        }
        return try {
            context.startService(intent)
            true
        } catch (e: SecurityException) {
            e.printStackTrace()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun openInNobiTerminal(context: Context, file: FileItem) {
        val directory: Path = if (file.attributes.isDirectory) {
            file.path
        } else {
            file.path.parent ?: return
        }
        if (!directory.isLinuxPath) {
            context.showToast(R.string.nobi_terminal_open_failed)
            return
        }
        if (!isInstalled(context.packageManager, NOBI_TERMINAL_PACKAGE)) {
            context.showToast(R.string.nobi_terminal_not_installed)
            return
        }
        if (!sendRunCommandIntent(context, directory)) {
            context.showToast(R.string.nobi_terminal_open_failed)
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
