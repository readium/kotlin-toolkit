/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.os.Bundle
import android.view.*
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.readium.r2.navigator.*
import org.readium.r2.navigator.media3.tts.android.AndroidTtsEngine
import org.readium.r2.navigator.util.BaseActionModeCallback
import org.readium.r2.navigator.util.EdgeTapNavigation
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Language
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentReaderBinding
import org.readium.r2.testapp.domain.model.Highlight
import org.readium.r2.testapp.reader.preferences.UserPreferencesBottomSheetDialogFragment
import org.readium.r2.testapp.reader.tts.TtsControls
import org.readium.r2.testapp.reader.tts.TtsViewModel
import org.readium.r2.testapp.utils.*
import org.readium.r2.testapp.utils.extensions.confirmDialog
import org.readium.r2.testapp.utils.extensions.throttleLatest

/*
 * Base reader fragment class
 *
 * Provides common menu items and saves last location on stop.
 */
@OptIn(ExperimentalDecorator::class, ExperimentalReadiumApi::class)
abstract class VisualReaderFragment : BaseReaderFragment(), VisualNavigator.Listener {

    protected var binding: FragmentReaderBinding by viewLifecycle()

    private lateinit var navigatorFragment: Fragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * When true, the user won't be able to interact with the navigator.
     */
    private var disableTouches by mutableStateOf(false)

    /**
     * When true, the fragment won't save progression.
     * This is useful in the case where the TTS is on and a service is saving progression
     * in background.
     */
    private var preventProgressionSaving: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navigatorFragment = navigator as Fragment

        setupObservers()

        childFragmentManager.addOnBackStackChangedListener {
            updateSystemUiVisibility()
        }
        binding.fragmentReaderContainer.setOnApplyWindowInsetsListener { container, insets ->
            updateSystemUiPadding(container, insets)
            insets
        }

        binding.overlay.setContent {
            if (disableTouches) {
                // Add an invisible box on top of the navigator to intercept touch gestures.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                requireActivity().toggleSystemUi()
                            }
                        }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                content = { Overlay() }
            )
        }
    }

    @Composable
    private fun BoxScope.Overlay() {
        model.tts?.let { tts ->
            TtsControls(
                model = tts,
                onPreferences = {
                    UserPreferencesBottomSheetDialogFragment(tts.preferencesModel, "TTS Settings")
                        .show(childFragmentManager, "TtsSettings")
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            )
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigator.currentLocator
                    .onEach {
                        if (!preventProgressionSaving) {
                            model.saveProgression(it)
                        }
                    }
                    .launchIn(this)

                setupHighlights(this)
                setupSearch(this)
                setupTts(this)
            }
        }
    }

    private suspend fun setupHighlights(scope: CoroutineScope) {
        (navigator as? DecorableNavigator)?.let { navigator ->
            navigator.addDecorationListener("highlights", decorationListener)

            model.highlightDecorations
                .onEach { navigator.applyDecorations(it, "highlights") }
                .launchIn(scope)
        }
    }

    private suspend fun setupSearch(scope: CoroutineScope) {
        (navigator as? DecorableNavigator)?.let { navigator ->
            model.searchDecorations
                .onEach { navigator.applyDecorations(it, "search") }
                .launchIn(scope)
        }
    }

    /**
     * Setup text-to-speech observers, if available.
     */
    private suspend fun setupTts(scope: CoroutineScope) {
        model.tts?.apply {
            events
                .onEach { event ->
                    when (event) {
                        is TtsViewModel.Event.OnError ->
                            showError(event.error)

                        is TtsViewModel.Event.OnMissingVoiceData ->
                            confirmAndInstallTtsVoice(event.language)
                    }
                }
                .launchIn(scope)

            // Navigate to the currently spoken word.
            // This will automatically turn pages when needed.
            position
                .filterNotNull()
                // Improve performances by throttling the moves to maximum one per second.
                .throttleLatest(1.seconds)
                .onEach { locator ->
                    navigator.go(locator, animated = false)
                }
                .launchIn(scope)

            // Prevent interacting with the publication (including page turns) while the TTS is
            // playing.
            isPlaying
                .onEach { isPlaying ->
                    disableTouches = isPlaying
                }
                .launchIn(scope)

            // Highlight the currently spoken utterance.
            (navigator as? DecorableNavigator)?.let { navigator ->
                highlight
                    .onEach { locator ->
                        val decoration = locator?.let {
                            Decoration(
                                id = "tts",
                                locator = it,
                                style = Decoration.Style.Highlight(tint = Color.RED)
                            )
                        }
                        navigator.applyDecorations(listOfNotNull(decoration), "tts")
                    }
                    .launchIn(scope)
            }

            showControls
                .onEach { showControls ->
                    preventProgressionSaving = showControls
                }
                .launchIn(scope)
        }
    }

    /**
     * Confirms with the user if they want to download the TTS voice data for the given language.
     */
    private suspend fun confirmAndInstallTtsVoice(language: Language) {
        val activity = activity ?: return
        val tts = model.tts ?: return

        if (
            activity.confirmDialog(
                getString(R.string.tts_error_language_support_incomplete, language.locale.displayLanguage)
            )
        ) {
            AndroidTtsEngine.requestInstallVoice(activity)
        }
    }

    override fun go(locator: Locator, animated: Boolean) {
        model.tts?.stop()
        super.go(locator, animated)
    }

    override fun onDestroyView() {
        (navigator as? DecorableNavigator)?.removeDecorationListener(decorationListener)
        super.onDestroyView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        setMenuVisibility(!hidden)
        requireActivity().invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater)
        menu.findItem(R.id.tts).isVisible = (model.tts != null)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.tts -> checkNotNull(model.tts).start(navigator)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // DecorableNavigator.Listener

    private val decorationListener by lazy { DecorationListener() }

    inner class DecorationListener : DecorableNavigator.Listener {
        override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
            val decoration = event.decoration
            // We stored the highlight's database ID in the `Decoration.extras` map, for
            // easy retrieval. You can store arbitrary information in the map.
            val id = (decoration.extras["id"] as Long)
                .takeIf { it > 0 } ?: return false

            // This listener will be called when tapping on any of the decorations in the
            // "highlights" group. To differentiate between the page margin icon and the
            // actual highlight, we check for the type of `decoration.style`. But you could
            // use any other information, including the decoration ID or the extras bundle.
            if (decoration.style is DecorationStyleAnnotationMark) {
                showAnnotationPopup(id)
            } else {
                event.rect?.let { rect ->
                    val isUnderline = (decoration.style is Decoration.Style.Underline)
                    showHighlightPopup(
                        rect,
                        style = if (isUnderline) Highlight.Style.UNDERLINE
                        else Highlight.Style.HIGHLIGHT,
                        highlightId = id
                    )
                }
            }

            return true
        }
    }

    // Highlights

    private var popupWindow: PopupWindow? = null
    private var mode: ActionMode? = null

    // Available tint colors for highlight and underline annotations.
    private val highlightTints = mapOf</*@IdRes*/ Int, /*@ColorInt*/ Int>(
        R.id.red to Color.rgb(247, 124, 124),
        R.id.green to Color.rgb(173, 247, 123),
        R.id.blue to Color.rgb(124, 198, 247),
        R.id.yellow to Color.rgb(249, 239, 125),
        R.id.purple to Color.rgb(182, 153, 255),
    )

    val customSelectionActionModeCallback: ActionMode.Callback by lazy { SelectionActionModeCallback() }

    private inner class SelectionActionModeCallback : BaseActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_action_mode, menu)
            if (navigator is DecorableNavigator) {
                menu.findItem(R.id.highlight).isVisible = true
                menu.findItem(R.id.underline).isVisible = true
                menu.findItem(R.id.note).isVisible = true
            }
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.highlight -> showHighlightPopupWithStyle(Highlight.Style.HIGHLIGHT)
                R.id.underline -> showHighlightPopupWithStyle(Highlight.Style.UNDERLINE)
                R.id.note -> showAnnotationPopup()
                else -> return false
            }

            mode.finish()
            return true
        }
    }

    private fun showHighlightPopupWithStyle(style: Highlight.Style) =
        viewLifecycleOwner.lifecycleScope.launch {
            // Get the rect of the current selection to know where to position the highlight
            // popup.
            (navigator as? SelectableNavigator)?.currentSelection()?.rect?.let { selectionRect ->
                showHighlightPopup(selectionRect, style)
            }
        }

    private fun showHighlightPopup(rect: RectF, style: Highlight.Style, highlightId: Long? = null) =
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (popupWindow?.isShowing == true) return@repeatOnLifecycle

                model.activeHighlightId.value = highlightId

                val isReverse = (rect.top > 60)
                val popupView = layoutInflater.inflate(
                    if (isReverse) R.layout.view_action_mode_reverse else R.layout.view_action_mode,
                    null,
                    false
                )
                popupView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )

                popupWindow = PopupWindow(
                    popupView,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    isFocusable = true
                    setOnDismissListener {
                        model.activeHighlightId.value = null
                    }
                }

                val x = rect.left
                val y = if (isReverse) rect.top else rect.bottom + rect.height()

                popupWindow?.showAtLocation(popupView, Gravity.NO_GRAVITY, x.toInt(), y.toInt())

                val highlight = highlightId?.let { model.highlightById(it) }
                popupView.run {
                    findViewById<View>(R.id.notch).run {
                        setX(rect.left * 2)
                    }

                    fun selectTint(view: View) {
                        val tint = highlightTints[view.id] ?: return
                        selectHighlightTint(highlightId, style, tint)
                    }

                    findViewById<View>(R.id.red).setOnClickListener(::selectTint)
                    findViewById<View>(R.id.green).setOnClickListener(::selectTint)
                    findViewById<View>(R.id.blue).setOnClickListener(::selectTint)
                    findViewById<View>(R.id.yellow).setOnClickListener(::selectTint)
                    findViewById<View>(R.id.purple).setOnClickListener(::selectTint)

                    findViewById<View>(R.id.annotation).setOnClickListener {
                        popupWindow?.dismiss()
                        showAnnotationPopup(highlightId)
                    }
                    findViewById<View>(R.id.del).run {
                        visibility = if (highlight != null) View.VISIBLE else View.GONE
                        setOnClickListener {
                            highlightId?.let {
                                model.deleteHighlight(highlightId)
                            }
                            popupWindow?.dismiss()
                            mode?.finish()
                        }
                    }
                }
            }
        }

    private fun selectHighlightTint(
        highlightId: Long? = null,
        style: Highlight.Style,
        @ColorInt tint: Int
    ) =
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (highlightId != null) {
                    model.updateHighlightStyle(highlightId, style, tint)
                } else {
                    (navigator as? SelectableNavigator)?.let { navigator ->
                        navigator.currentSelection()?.let { selection ->
                            model.addHighlight(
                                locator = selection.locator,
                                style = style,
                                tint = tint
                            )
                        }
                        navigator.clearSelection()
                    }
                }

                popupWindow?.dismiss()
                mode?.finish()
            }
        }

    private fun showAnnotationPopup(highlightId: Long? = null) =
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val activity = activity ?: return@repeatOnLifecycle
                val view = layoutInflater.inflate(R.layout.popup_note, null, false)
                val note = view.findViewById<EditText>(R.id.note)
                val alert = AlertDialog.Builder(activity)
                    .setView(view)
                    .create()

                fun dismiss() {
                    alert.dismiss()
                    mode?.finish()
                    (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(
                            note.applicationWindowToken,
                            InputMethodManager.HIDE_NOT_ALWAYS
                        )
                }

                with(view) {
                    val highlight = highlightId?.let { model.highlightById(it) }
                    if (highlight != null) {
                        note.setText(highlight.annotation)
                        findViewById<View>(R.id.sidemark).setBackgroundColor(highlight.tint)
                        findViewById<TextView>(R.id.select_text).text =
                            highlight.locator.text.highlight

                        findViewById<TextView>(R.id.positive).setOnClickListener {
                            val text = note.text.toString()
                            model.updateHighlightAnnotation(highlight.id, annotation = text)
                            dismiss()
                        }
                    } else {
                        val tint = highlightTints.values.random()
                        findViewById<View>(R.id.sidemark).setBackgroundColor(tint)
                        val navigator =
                            navigator as? SelectableNavigator ?: return@repeatOnLifecycle
                        val selection = navigator.currentSelection() ?: return@repeatOnLifecycle
                        navigator.clearSelection()
                        findViewById<TextView>(R.id.select_text).text =
                            selection.locator.text.highlight

                        findViewById<TextView>(R.id.positive).setOnClickListener {
                            model.addHighlight(
                                locator = selection.locator,
                                style = Highlight.Style.HIGHLIGHT,
                                tint = tint,
                                annotation = note.text.toString()
                            )
                            dismiss()
                        }
                    }

                    findViewById<TextView>(R.id.negative).setOnClickListener {
                        dismiss()
                    }
                }

                alert.show()
            }
        }

    fun updateSystemUiVisibility() {
        if (navigatorFragment.isHidden)
            requireActivity().showSystemUi()
        else
            requireActivity().hideSystemUi()

        requireView().requestApplyInsets()
    }

    private fun updateSystemUiPadding(container: View, insets: WindowInsets) {
        if (navigatorFragment.isHidden) {
            container.padSystemUi(insets, requireActivity() as AppCompatActivity)
        } else {
            container.clearPadding()
        }
    }

    // VisualNavigator.Listener

    override fun onTap(point: PointF): Boolean {
        val navigated = edgeTapNavigation.onTap(point, requireView())

        if (!navigated) {
            requireActivity().toggleSystemUi()
        }
        return true
    }

    private val edgeTapNavigation by lazy {
        EdgeTapNavigation(
            navigator = navigator as VisualNavigator
        )
    }
}

/**
 * Decoration Style for a page margin icon.
 *
 * This is an example of a custom Decoration Style declaration.
 */
@Parcelize
@OptIn(ExperimentalDecorator::class)
data class DecorationStyleAnnotationMark(@ColorInt val tint: Int) : Decoration.Style

/**
 * Decoration Style for a page number label.
 *
 * This is an example of a custom Decoration Style declaration.
 *
 * @param label Page number label as declared in the `page-list` link object.
 */
@Parcelize
@OptIn(ExperimentalDecorator::class)
data class DecorationStylePageNumber(val label: String) : Decoration.Style
