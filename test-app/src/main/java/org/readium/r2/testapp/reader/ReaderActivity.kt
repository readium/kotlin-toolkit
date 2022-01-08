/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.Application
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.ActivityReaderBinding
import org.readium.r2.testapp.drm.DrmManagementContract
import org.readium.r2.testapp.drm.DrmManagementFragment
import org.readium.r2.testapp.outline.OutlineContract
import org.readium.r2.testapp.outline.OutlineFragment

/*
 * An activity to read a publication
 *
 * This class can be used as it is or be inherited from.
 */
open class ReaderActivity : AppCompatActivity() {

    private lateinit var modelFactory: ReaderViewModel.Factory
    private lateinit var model: ReaderViewModel
    private lateinit var binding: ActivityReaderBinding
    private lateinit var readerFragment: BaseReaderFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        val bookId = ReaderContract.parseIntent(this)
        val app = applicationContext as Application
        modelFactory = ReaderViewModel.Factory(app, bookId)
        super.onCreate(savedInstanceState)

        val binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        this.binding = binding
        this.model = ViewModelProvider(this)[ReaderViewModel::class.java]
        
        lifecycleScope.launch {
            model.argumentsDiffered.await()
                .onFailure { exception -> onLoadingError(exception) }
                .onSuccess { arguments -> onPublicationAvailable(arguments) }
        }

        supportFragmentManager.setFragmentResultListener(
            OutlineContract.REQUEST_KEY,
            this,
            FragmentResultListener { _, result ->
                val locator = OutlineContract.parseResult(result).destination
                closeOutlineFragment(locator)
            }
        )

        supportFragmentManager.setFragmentResultListener(
            DrmManagementContract.REQUEST_KEY,
            this,
            FragmentResultListener { _, result ->
                if (DrmManagementContract.parseResult(result).hasReturned)
                    finish()
            }
        )

        supportFragmentManager.addOnBackStackChangedListener {
            reconfigureActionBar()
        }

        // Add support for display cutout.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun onLoadingError(exception: Exception) {
    }

    private fun onPublicationAvailable(arguments: PublicationRepository.ReaderArguments) {
        val readerFragment = supportFragmentManager.findFragmentByTag(READER_FRAGMENT_TAG)
            ?.let { it as BaseReaderFragment }
            ?: run { createReaderFragment(arguments) }

        if (readerFragment is FullscreenReaderFragment) {
            val fullscreenDelegate = FullscreenReaderActivityDelegate(this, readerFragment, binding)
            lifecycle.addObserver(fullscreenDelegate)
        }

        this.readerFragment = readerFragment
        model.activityChannel.receive(this) { handleReaderFragmentEvent(it) }
        reconfigureActionBar()
    }

    private fun createReaderFragment(arguments: PublicationRepository.ReaderArguments): BaseReaderFragment {
        val readerClass: Class<out Fragment> = when {
            arguments.publication.conformsTo(Publication.Profile.EPUB) -> EpubReaderFragment::class.java
            arguments.publication.conformsTo(Publication.Profile.PDF) -> PdfReaderFragment::class.java
            arguments.publication.conformsTo(Publication.Profile.DIVINA) -> ImageReaderFragment::class.java
            arguments.publication.conformsTo(Publication.Profile.AUDIOBOOK) -> AudioReaderFragment::class.java
            else -> throw IllegalArgumentException("Cannot render publication")
        }

        supportFragmentManager.commitNow {
            replace(R.id.activity_container, readerClass, Bundle(), READER_FRAGMENT_TAG)
        }

        return supportFragmentManager.findFragmentByTag(READER_FRAGMENT_TAG) as BaseReaderFragment
    }

    override fun onStart() {
        super.onStart()
        reconfigureActionBar()
    }

    private fun reconfigureActionBar() {
        val currentFragment = supportFragmentManager.fragments.lastOrNull()

        title = when (currentFragment) {
            is OutlineFragment -> model.arguments.publication.metadata.title
            is DrmManagementFragment -> getString(R.string.title_fragment_drm_management)
            else -> null
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(
            when (currentFragment) {
                is OutlineFragment, is DrmManagementFragment -> true
                else -> false

            }
        )
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return modelFactory
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, intent)
        super.finish()
    }

    private fun handleReaderFragmentEvent(event: ReaderViewModel.Event) {
        when(event) {
            is ReaderViewModel.Event.OpenOutlineRequested -> showOutlineFragment()
            is ReaderViewModel.Event.OpenDrmManagementRequested -> showDrmManagementFragment()
            is ReaderViewModel.Event.Failure -> {
                Toast.makeText(this, event.error.getUserMessage(this), Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    private fun showOutlineFragment() {
        supportFragmentManager.commit {
            add(R.id.activity_container, OutlineFragment::class.java, Bundle(), OUTLINE_FRAGMENT_TAG)
            hide(readerFragment)
            addToBackStack(null)
        }
    }

    private fun closeOutlineFragment(locator: Locator) {
        readerFragment.go(locator, true)
        supportFragmentManager.popBackStack()
    }

    private fun showDrmManagementFragment() {
        supportFragmentManager.commit {
            add(
                R.id.activity_container,
                DrmManagementFragment::class.java,
                Bundle(),
                DRM_FRAGMENT_TAG
            )
            hide(readerFragment)
            addToBackStack(null)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                supportFragmentManager.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val READER_FRAGMENT_TAG = "reader"
        const val OUTLINE_FRAGMENT_TAG = "outline"
        const val DRM_FRAGMENT_TAG = "drm"
    }
}
