package org.readium.r2.testapp.reader

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.IntSize
import androidx.fragment.app.activityViewModels
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator2.view.NavigatorListener
import org.readium.r2.navigator3.Navigator
import org.readium.r2.shared.publication.Locator
import org.readium.r2.testapp.databinding.Reader3FragmentReaderBinding
import org.readium.r2.testapp.utils.viewLifecycle

class ComposeReaderFragment : VisualReaderFragment(), VisualNavigator.Listener {

    private var binding: Reader3FragmentReaderBinding by viewLifecycle()
    override val model: ReaderViewModel by activityViewModels()
    private val readerData by lazy { model.readerInitData as ComposeVisualReaderInitData }
    override val navigator: VisualNavigator by lazy { ComposeNavigatorAdapter(readerData.navigatorState) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = Reader3FragmentReaderBinding.inflate(inflater, container, false)
        binding.fragmentReader3Navigator.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                Navigator(
                    state = remember { readerData.navigatorState },
                    onTap = { offset -> onTap(PointF(offset.x, offset.y)) }
                )
            }
        }
        this.binding = binding
        return binding.root
    }
}
