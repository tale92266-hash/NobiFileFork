/*
 * Copyright (c) 2026 NobiFile contributors
 *
 * Sidebar entries that open a companion app's storage inside NobiFile
 * instead of launching the app: Nobi Code's workspace folder and Nobi
 * Terminal's home directory, each exposed through the app's own
 * DocumentsProvider. Both need a one-time Storage Access Framework
 * grant; until Sam grants it, tapping the entry opens the system folder
 * picker prefilled at the target folder. Nothing here assumes the grant
 * exists. (Device evidence, 2026-11-01 grant log: Nobi Code's provider
 * is com.nobicode.editor.documents, not the external-storage provider.)
 */

package me.zhanghai.android.files.navigation

import android.net.Uri
import android.provider.DocumentsContract
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nobifile.manager.R
import me.zhanghai.android.files.app.contentResolver
import me.zhanghai.android.files.file.asDocumentTreeUriOrNull

enum class NobiTreeTarget(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    val authority: String,
    val documentId: String
) {
    NOBI_CODE(
        R.drawable.nobi_nav_nobicode, R.string.navigation_nobi_code,
        "com.nobicode.editor.documents",
        "/data/user/0/com.nobicode.editor/files/public/NobiCode"
    ),
    NOBI_TERMINAL(
        R.drawable.nobi_nav_nobiterminal, R.string.navigation_nobi_terminal,
        "com.nobiterminal.documents", "/data/data/com.nobiterminal/files/home"
    );

    val treeUri: Uri
        get() = DocumentsContract.buildTreeDocumentUri(authority, documentId)

    val initialDocumentUri: Uri
        get() = DocumentsContract.buildDocumentUri(authority, documentId)

    fun findCoveringGrant(): Uri? {
        val targetTreeUriString = treeUri.toString()
        val wantedId = documentId.trimEnd('/')
        for (permission in contentResolver.persistedUriPermissions) {
            if (!permission.isReadPermission) {
                continue
            }
            val grantedTreeUri = permission.uri.asDocumentTreeUriOrNull() ?: continue
            if (grantedTreeUri.value.authority != authority) {
                continue
            }
            val grantedDocumentId = try {
                DocumentsContract.getTreeDocumentId(grantedTreeUri.value)
            } catch (e: IllegalArgumentException) {
                null
            } ?: continue
            val grantedId = grantedDocumentId.trimEnd('/')
            val grantedTreeUriString = grantedTreeUri.value.toString()
            if (grantedId == wantedId ||
                wantedId.startsWith("$grantedId/") ||
                targetTreeUriString.startsWith(grantedTreeUriString) ||
                grantedTreeUriString.startsWith(targetTreeUriString)
            ) {
                return grantedTreeUri.value
            }
        }
        return null
    }
}
