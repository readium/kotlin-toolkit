package org.readium.r2.testapp.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.image.ImageNavigatorFragment
import org.readium.r2.shared.publication.Publication
import org.readium.r2.testapp.R
import org.readium.r2.testapp.audiobook.AudioNavigatorFragment

class AudioReaderFragment : BaseReaderFragment(), AudioNavigatorFragment.Listener {

    override lateinit var model: ReaderViewModel
    override lateinit var navigator: Navigator
    private lateinit var publication: Publication
    private lateinit var persistence: BookData

    override fun onCreate(savedInstanceState: Bundle?) {
        val activity = requireActivity()

        ViewModelProvider(activity).get(ReaderViewModel::class.java).let {
            model = it
            publication = it.publication
            persistence = it.persistence
        }

        childFragmentManager.fragmentFactory =
            AudioNavigatorFragment.createFactory(publication, persistence.savedLocation, this)

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(R.id.fragment_reader_container, AudioNavigatorFragment::class.java, Bundle(), NAVIGATOR_FRAGMENT_TAG)
            }
        }
        navigator = childFragmentManager.findFragmentByTag(NAVIGATOR_FRAGMENT_TAG)!! as Navigator
        return view
    }

    companion object {

        const val NAVIGATOR_FRAGMENT_TAG = "navigator"
    }
}