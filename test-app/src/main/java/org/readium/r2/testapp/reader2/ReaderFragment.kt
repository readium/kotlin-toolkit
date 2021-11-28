package org.readium.r2.testapp.reader2

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.readium.r2.lcp.lcpLicense
import org.readium.r2.navigator2.view.NavigatorConfiguration
import org.readium.r2.navigator2.view.NavigatorListener
import org.readium.r2.navigator2.view.NavigatorView
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.presentation.Presentation
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.Reader2FragmentReaderBinding
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.toggleSystemUi
import timber.log.Timber
import java.net.URL

class ReaderFragment : VisualReaderFragment(), NavigatorListener {

    override var binding: Reader2FragmentReaderBinding? = null
    private lateinit var model: ReaderViewModel
    private lateinit var publication: Publication
    private lateinit var navigator: NavigatorView
    private var baseUrl: URL? = null

    private val currentResource: Int
        get() {
            val current = this.navigator.currentLocation.href
            return this.publication.readingOrder.indexOfFirstWithHref(current)!!
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            model = it
            publication = it.publication
            baseUrl = it.baseUrl
        }

        super.onCreate(savedInstanceState)

        model.fragmentChannel.receive(this) { event ->
            val message =
                when (event) {
                    is ReaderViewModel.FeedbackEvent.BookmarkFailed -> R.string.bookmark_exists
                    is ReaderViewModel.FeedbackEvent.BookmarkSuccessfullyAdded -> R.string.bookmark_added
                }
            Toast.makeText(requireContext(), getString(message), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_reader, menu)
        menu.findItem(R.id.drm).isVisible = model.publication.lcpLicense != null
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        setMenuVisibility(!hidden)
        requireActivity().invalidateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toc -> {
                model.channel.send(ReaderViewModel.Event.OpenOutlineRequested)
                true
            }
            R.id.bookmark -> {
                val locator = navigator.currentLocation
                model.insertBookmark(locator)
                true
            }
            R.id.drm -> {
                model.channel.send(ReaderViewModel.Event.OpenDrmManagementRequested)
                true
            }
            else -> false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = Reader2FragmentReaderBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigator = view.findViewById(R.id.fragment_reader2_navigator)
        navigator.listener = this
        navigator.settings = navigator.settings.copy(
            spread = Presentation.Spread.LANDSCAPE,
            continuous = true,
            overflow = Presentation.Overflow.SCROLLED,
            readingProgression = ReadingProgression.TTB
        )
        navigator.configuration = NavigatorConfiguration(baseUrl)
        navigator.loadPublication(publication)

        model.location?.let {
            Timber.d("Restoring last location saved $it")
            viewLifecycleOwner.lifecycleScope.launch { navigator.goTo(it) }
        }
    }

    override fun onTap(point: PointF): Boolean {
        if (navigator.properties.continuous) {
            requireActivity().toggleSystemUi()
        } else {
            val viewWidth = requireView().width
            val leftRange = 0.0..(0.2 * viewWidth)

            when {
                leftRange.contains(point.x) -> this.goBackward()
                leftRange.contains(viewWidth - point.x) -> this.goForward()
                else -> requireActivity().toggleSystemUi()
            }
        }

        return true
    }

    private fun goForward() {
        if (currentResource + 1 >= this.publication.readingOrder.size) {
            Timber.d("Current resource $currentResource")
            Timber.d("Reached the end of the publication. Can't go forward.")
            return
        }

        val newLocation = this.publication.readingOrder[currentResource + 1].toLocator()
        this.go(newLocation)
    }

    private fun goBackward() {
        if (currentResource - 1 < 0) {
            Timber.d("Current resource $currentResource")
            Timber.d("Reached the beginning of the publication. Can't go backward.")
            return
        }

        val newLocation = this.publication.readingOrder[currentResource - 1].toLocator()
        this.go(newLocation)
    }

    fun go(locator: Locator) {
        model.location = locator
        viewLifecycleOwner.lifecycleScope.launch {
            navigator.goTo(locator)
        }
    }

    override fun onLocationChanged(newLocation: Locator) {
        Timber.d("New location $newLocation")
        this.model.location = newLocation
        this.model.saveProgression(newLocation)
    }

    override fun onDestroyView() {
        Timber.d("Last location saved ${this.model.location}")
        binding = null
        this.viewLifecycleOwner.lifecycleScope.cancel()
        super.onDestroyView()
    }
}