package org.readium.r2.testapp.reader

import android.graphics.PointF
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.readium.r2.lcp.lcpLicense
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator2.view.NavigatorListener
import org.readium.r2.navigator3.Navigator
import org.readium.r2.navigator3.NavigatorState
import org.readium.r2.navigator3.Overflow
import org.readium.r2.navigator3.ReadingProgression
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.publication.toLocator
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.Reader3FragmentReaderBinding
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.reader.VisualReaderFragment
import org.readium.r2.testapp.utils.toggleSystemUi
import org.readium.r2.testapp.utils.viewLifecycle
import timber.log.Timber
import java.net.URL

class ComposeReaderFragment : VisualReaderFragment(), NavigatorListener {

    private var binding: Reader3FragmentReaderBinding by viewLifecycle()
    override val model: ReaderViewModel by activityViewModels()
    private val readerData by lazy { model.readerInitData as ComposeVisualReaderInitData }
    override val navigator: Navigator by lazy { ComposeNavigatorAdapter(readerData.navigatorState) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = Reader3FragmentReaderBinding.inflate(inflater, container, false)
        binding.fragmentReader3Navigator.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                val state = remember { readerData.navigatorState }
                Navigator(state = state)
            }
        }
        this.binding = binding
        return binding.root
    }

    override fun onLocationChanged(newLocation: Locator) {
        // Pass
    }
}
