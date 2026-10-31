/*
 * Copyright (c) 2026 NobiFile contributors
 *
 * Entry-point splash activity: shows the branded splash window background
 * and immediately forwards to FileListActivity, preserving the original
 * intent (action, data and extras) so folder shortcuts and VIEW intents
 * keep working. It is finished before the file list appears and never
 * stays in the back stack.
 */

package me.zhanghai.android.files.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.zhanghai.android.files.filelist.FileListActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(intent).setClass(this, FileListActivity::class.java))
        finish()
    }
}
