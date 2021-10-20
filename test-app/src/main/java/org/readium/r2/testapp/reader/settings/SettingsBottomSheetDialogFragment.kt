package org.readium.r2.testapp.reader.settings

import androidx.compose.runtime.Composable
import androidx.fragment.app.activityViewModels
import org.readium.r2.navigator.ExperimentalPresentation
import org.readium.r2.navigator.presentation.PresentationController
import org.readium.r2.testapp.reader.ReaderViewModel
import org.readium.r2.testapp.utils.compose.ComposeBottomSheetDialogFragment

@OptIn(ExperimentalPresentation::class)
abstract class SettingsBottomSheetDialogFragment : ComposeBottomSheetDialogFragment() {

    protected val model: ReaderViewModel by activityViewModels()

    protected val presentation: PresentationController?
        get() = model.presentation
}

@OptIn(ExperimentalPresentation::class)
class FixedSettingsBottomSheetDialogFragment : SettingsBottomSheetDialogFragment() {
    @Composable
    override fun Content() {
        FixedSettingsView(presentation = requireNotNull(presentation))
    }
}
