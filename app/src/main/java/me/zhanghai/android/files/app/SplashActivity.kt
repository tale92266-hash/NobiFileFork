/*
 * Copyright (c) 2026 NobiFile contributors
 *
 * Entry-point splash activity: shows the branded splash window background
 * and immediately forwards to FileListActivity with a fresh intent.
 * Folder shortcuts and VIEW intents resolve directly to FileListActivity
 * through its own intent filters, so nothing is lost. It is finished
 * before the file list appears and never stays in the back stack.
 *
 * Extends AppActivity on purpose: CustomThemeHelper.initialize() throws
 * for any activity that does not call apply() from AppActivity.onCreate.
 * The splash window background is drawn from the manifest theme before
 * onCreate, so apply() replacing the theme here changes nothing visible.
 */

package me.zhanghai.android.files.app

import android.content.Intent
import android.os.Bundle
import me.zhanghai.android.files.filelist.FileListActivity

class SplashActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, FileListActivity::class.java))
        finish()
    }
}
