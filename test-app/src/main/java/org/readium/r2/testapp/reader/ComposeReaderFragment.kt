package org.readium.r2.testapp.reader

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator3.Navigator

class ComposeReaderFragment : VisualReaderFragment(), VisualNavigator.Listener {

    private val readerData by lazy { model.readerInitData as ComposeVisualReaderInitData }
    override val navigator: ComposeNavigatorAdapter by lazy { ComposeNavigatorAdapter(readerData.navigatorState) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

       binding.composeNavigator.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                Navigator(
                    state = remember { readerData.navigatorState },
                    onTap = { offset -> onTap(PointF(offset.x, offset.y)) }
                )
            }
        }
        return binding.root
    }
}
