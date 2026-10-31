/*
 * Copyright (c) 2026 NobiFile contributors
 *
 * Sidebar entries that open a companion app's storage inside NobiFile
 * instead of launching the app: Nobi Code's folder under Android/data, and
 * Nobi Terminal's home directory exposed through its own DocumentsProvider.
 * Both need a one-time Storage Access Framework grant; until Sam grants it,
 * tapping the entry opens the system folder picker prefilled at the target
 * folder. Nothing here assumes the grant exists.
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
        "com.android.externalstorage.documents", "primary:Android/data/com.nobicode.editor"
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
        for (permission in contentResolver.persistedUriPermissions) {
            if (!permission.isReadPermission) {
                continue
            }
            val grantedTreeUri = permission.uri.asDocumentTreeUriOrNull() ?: continue
            if (grantedTreeUri.value.authority != authority) {
                continue
            }
            val grantedDocumentId = DocumentsContract.getTreeDocumentId(grantedTreeUri.value)
            if (grantedDocumentId == documentId ||
                documentId.startsWith("$grantedDocumentId/")
            ) {
                return grantedTreeUri.value
            }
        }
        return null
    }
}
