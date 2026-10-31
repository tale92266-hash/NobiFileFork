/*
 * Copyright (c) 2026 NobiFile contributors
 *
 * Entry-point splash activity: shows the branded splash window background
 * and immediately forwards to FileListActivity with a fresh intent.
 * Folder shortcuts and VIEW intents resolve directly to FileListActivity
 * through its own intent filters, so nothing is lost. It is finished
 * before the file list appears and never stays in the back stack.
 */

package me.zhanghai.android.files.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.zhanghai.android.files.filelist.FileListActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, FileListActivity::class.java))
        finish()
    }
}
