package org.readium.r2.testapp.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commitNow
import org.readium.navigator.web.WebNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.testapp.R

@OptIn(ExperimentalReadiumApi::class)
class WebReaderFragment : VisualReaderFragment(), EpubNavigatorFragment.Listener {

    override lateinit var navigator: org.readium.navigator.web.WebNavigatorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        val readerData = model.readerInitData as WebReaderInitData

        childFragmentManager.fragmentFactory =
            readerData.navigatorFactory.createFragmentFactory(
                initialLocator = readerData.initialLocation
            )

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(
                    R.id.fragment_reader_container,
                    org.readium.navigator.web.WebNavigatorFragment::class.java,
                    Bundle(),
                    NAVIGATOR_FRAGMENT_TAG
                )
            }
        }
        navigator = childFragmentManager.findFragmentByTag(NAVIGATOR_FRAGMENT_TAG) as org.readium.navigator.web.WebNavigatorFragment

        return view
    }

    companion object {
        private const val NAVIGATOR_FRAGMENT_TAG = "navigator"
    }
}
