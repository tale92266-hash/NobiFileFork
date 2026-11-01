/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.navigation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import java8.nio.file.Path
import com.nobifile.manager.R
import com.nobifile.manager.databinding.NavigationFragmentBinding
import me.zhanghai.android.files.file.asDocumentTreeUriOrNull
import me.zhanghai.android.files.file.takePersistablePermission
import me.zhanghai.android.files.provider.document.createDocumentTreeRootPath
import me.zhanghai.android.files.util.showToast
import me.zhanghai.android.files.util.startActivitySafe

class NavigationFragment : Fragment(), NavigationItem.Listener {
    private lateinit var binding: NavigationFragmentBinding

    private lateinit var adapter: NavigationListAdapter

    lateinit var listener: Listener

    private var pendingNobiTreeTarget: NobiTreeTarget? = null

    private val openNobiTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(), this::onOpenNobiTreeResult
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        NavigationFragmentBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // The header sits above the list, so it owns the status-bar inset:
        // its top padding is the base padding plus the status-bar height,
        // and the insets pass through unconsumed for the list below.
        val headerTopPadding = binding.headerLayout.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.headerLayout) { view, insets ->
            view.updatePadding(
                top = headerTopPadding +
                    insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            )
            insets
        }

        binding.recyclerView.setHasFixedSize(true)
        // TODO: Needed?
        //binding.recyclerView.setItemAnimator(new NoChangeAnimationItemAnimator())
        val context = requireContext()
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = NavigationListAdapter(this, context)
        binding.recyclerView.adapter = adapter

        val viewLifecycleOwner = viewLifecycleOwner
        NavigationItemListLiveData.observe(viewLifecycleOwner) { onNavigationItemsChanged(it) }
        listener.observeCurrentPath(viewLifecycleOwner) { onCurrentPathChanged(it) }
    }

    private fun onNavigationItemsChanged(navigationItems: List<NavigationItem?>) {
        adapter.replace(navigationItems)
    }

    private fun onCurrentPathChanged(path: Path) {
        adapter.notifyCheckedChanged()
    }

    override val currentPath: Path
        get() = listener.currentPath

    override fun navigateTo(path: Path) {
        listener.navigateTo(path)
    }

    override fun navigateToRoot(path: Path) {
        listener.navigateToRoot(path)
    }

    override fun launchIntent(intent: Intent) {
        startActivitySafe(intent)
    }

    override fun openNobiTree(target: NobiTreeTarget) {
        pendingNobiTreeTarget = target
        // Same picker contract the working "Add storage" flow uses: the
        // stock OpenDocumentTree contract with the target as the initial
        // URI. A hand-built ACTION_OPEN_DOCUMENT_TREE request did not
        // leave a persisted grant on this device, so every tap re-asked.
        openNobiTreeLauncher.launch(target.initialDocumentUri)
    }

    private fun onOpenNobiTreeResult(returnedUri: Uri?) {
        val target = pendingNobiTreeTarget
        pendingNobiTreeTarget = null
        if (target == null) {
            return
        }
        val treeUri = returnedUri?.asDocumentTreeUriOrNull()
        if (treeUri == null) {
            NobiTreeGrantDebug.log(
                "RESULT target=${target.name} returnedUri=$returnedUri (cancelled or not a tree)"
            )
            return
        }
        // Take the grant (read+write, falling back to read-only). If the
        // system did not offer a persistable grant, this returns false.
        val persisted = treeUri.takePersistablePermission()
        NobiTreeGrantDebug.log(
            "RESULT target=${target.name} returnedUri=$returnedUri " +
                "persisted=$persisted grants=[${NobiTreeGrantDebug.describeGrants()}] " +
                "coveringGrant=${target.findCoveringGrant()}"
        )
        if (!persisted) {
            showToast(R.string.nobi_tree_permission_not_saved)
        }
        listener.navigateTo(treeUri.value.createDocumentTreeRootPath())
        listener.closeNavigationDrawer()
    }

    override fun closeNavigationDrawer() {
        listener.closeNavigationDrawer()
    }

    interface Listener {
        val currentPath: Path
        fun navigateTo(path: Path)
        fun navigateToRoot(path: Path)
        fun navigateToDefaultRoot()
        fun observeCurrentPath(owner: LifecycleOwner, observer: (Path) -> Unit)
        fun closeNavigationDrawer()
    }
}
