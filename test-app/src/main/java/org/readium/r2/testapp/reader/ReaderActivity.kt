/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.allAreAudio
import org.readium.r2.shared.publication.allAreBitmap
import org.readium.r2.shared.util.mediatype.MediaType
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

    protected lateinit var readerFragment: BaseReaderFragment
    private lateinit var modelFactory: ReaderViewModel.Factory
    private lateinit var publication: Publication

    lateinit var binding: ActivityReaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val inputData = ReaderContract.parseIntent(this)
        modelFactory = ReaderViewModel.Factory(applicationContext, inputData)
        super.onCreate(savedInstanceState)

        binding = ActivityReaderBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        ViewModelProvider(this).get(ReaderViewModel::class.java).let { model ->
            publication = model.publication
            model.channel.receive(this) { handleReaderFragmentEvent(it) }
        }

        if (savedInstanceState == null) {

            if (publication.type == Publication.TYPE.EPUB) {
                val baseUrl = requireNotNull(inputData.baseUrl)
                readerFragment = EpubReaderFragment.newInstance(baseUrl)

                supportFragmentManager.commitNow {
                    replace(R.id.activity_container, readerFragment, READER_FRAGMENT_TAG)
                }
            } else {
                val readerClass: Class<out Fragment> = when {
                    publication.readingOrder.all { it.mediaType == MediaType.PDF } -> PdfReaderFragment::class.java
                    publication.readingOrder.allAreBitmap -> ImageReaderFragment::class.java
                    publication.readingOrder.allAreAudio -> AudioReaderFragment::class.java
                    else -> throw IllegalArgumentException("Cannot render publication")
                }

                supportFragmentManager.commitNow {
                    replace(R.id.activity_container, readerClass, Bundle(), READER_FRAGMENT_TAG)
                }
            }
        }

        readerFragment = supportFragmentManager.findFragmentByTag(READER_FRAGMENT_TAG) as BaseReaderFragment

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
            updateActivityTitle()
        }

        // Add support for display cutout.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    override fun onStart() {
        super.onStart()
        updateActivityTitle()
    }

    private fun updateActivityTitle() {
        title = when (supportFragmentManager.fragments.last()) {
            is OutlineFragment -> publication.metadata.title
            is DrmManagementFragment -> getString(R.string.title_fragment_drm_management)
            else -> null
        }
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
            add(R.id.activity_container, DrmManagementFragment::class.java, Bundle(), DRM_FRAGMENT_TAG)
            hide(readerFragment)
            addToBackStack(null)
        }
    }

    companion object {
        const val READER_FRAGMENT_TAG = "reader"
        const val OUTLINE_FRAGMENT_TAG = "outline"
        const val DRM_FRAGMENT_TAG = "drm"
    }
}
