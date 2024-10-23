package org.readium.navigator.pdf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.viewModels
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.readium.navigator.common.InputListener
import org.readium.navigator.common.Preferences
import org.readium.navigator.common.Settings
import org.readium.navigator.common.TapContext
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

@ExperimentalReadiumApi
@Composable
public fun <S : Settings, P : Preferences<P>> PdfNavigator(
    modifier: Modifier = Modifier,
    state: PdfNavigatorState<S, P>,
    inputListener: InputListener
) {
    val preferencesFlow = snapshotFlow { state.preferences.value }

    val pendingPositionFlow = snapshotFlow { state.pendingLocator.value }

    AndroidFragment<PdfFragment<S, P>>(
        modifier = modifier,
        onUpdate = {
            val onFragmentCreated: (PdfNavigatorFragment<S, P>) -> Unit = { fragment ->

                val legacyInputListener =
                    object : org.readium.r2.navigator.input.InputListener {
                        override fun onTap(event: TapEvent): Boolean {
                            val viewport = DpSize(
                                width = fragment.publicationView.width.toFloat().dp,
                                height = fragment.publicationView.height.toFloat().dp
                            )
                            val offset = DpOffset(event.point.x.dp, event.point.y.dp)
                            val dpEvent = org.readium.navigator.common.TapEvent(offset)
                            inputListener.onTap(dpEvent, TapContext(viewport))
                            return true
                        }
                    }
                fragment.addInputListener(legacyInputListener)

                fragment.currentLocator
                    .onEach { locator ->
                        state.locator.value = locator
                    }.launchIn(fragment.lifecycleScope)

                preferencesFlow
                    .onEach { preferences -> fragment.submitPreferences(preferences) }
                    .launchIn(fragment.lifecycleScope)

                pendingPositionFlow
                    .onEach { locator ->
                        if (locator != null) {
                            fragment.go(locator)
                            state.pendingLocator.value = null
                        }
                    }
                    .launchIn(fragment.lifecycleScope)
            }

            it.setNavigatorFactory(
                state.pdfNavigatorFactory,
                state.locator.value,
                state.preferences.value,
                onFragmentCreated
            )
        }
    )
}

@OptIn(ExperimentalReadiumApi::class)
public // Visible for Android
class PdfFragment<S : Settings, P : Preferences<P>> : Fragment() {

    private val viewModel: PdfFragmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory = viewModel.fragmentFactory
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragmentContainer = FragmentContainerView(requireContext())
        fragmentContainer.layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        fragmentContainer.id = viewModel.navigatorViewId
        return fragmentContainer
    }

    internal fun setNavigatorFactory(
        navigatorFactory: PdfNavigatorFactory<S, P, *>,
        locator: Locator,
        preferences: P,
        onFragmentCreated: (fragment: PdfNavigatorFragment<S, P>) -> Unit
    ) {
        viewModel.fragmentFactory.factory =
            navigatorFactory.createFragmentFactory(
                initialLocator = locator,
                initialPreferences = preferences
            )

        childFragmentManager.beginTransaction()
            .replace(
                viewModel.navigatorViewId,
                PdfNavigatorFragment::class.java,
                Bundle(),
                "PdfNavigator"
            )
            .commitNow()

        @Suppress("Unchecked_cast")
        val fragment = childFragmentManager.fragments[0] as PdfNavigatorFragment<S, P>
        onFragmentCreated(fragment)
    }
}

@ExperimentalReadiumApi
public class PdfFragmentViewModel : ViewModel() {

    internal val navigatorViewId: Int = View.generateViewId()

    internal val fragmentFactory = MutableFragmentFactory(CoolFragmentFactory())
}

private class CoolFragmentFactory : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return object : Fragment() {

            override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View {
                return FrameLayout(requireContext())
            }
        }
    }
}

public class MutableFragmentFactory(public var factory: FragmentFactory? = null) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
        when (val factoryNow = factory) {
            null -> super.instantiate(classLoader, className)
            else -> factoryNow.instantiate(classLoader, className)
        }
}
