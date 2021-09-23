/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.reader

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import org.readium.r2.lcp.lcpLicense
import org.readium.r2.navigator.*
import org.readium.r2.navigator.util.BaseActionModeCallback
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isProtected
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentReaderBinding
import org.readium.r2.testapp.domain.model.Highlight

/*
 * Base reader fragment class
 *
 * Provides common menu items and saves last location on stop.
 */
@OptIn(ExperimentalDecorator::class)
abstract class BaseReaderFragment : Fragment() {

    protected abstract val model: ReaderViewModel
    protected abstract val navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewScope = viewLifecycleOwner.lifecycleScope

        navigator.currentLocator
            .onEach { model.saveProgression(it) }
            .launchIn(viewScope)

        (navigator as? DecorableNavigator)?.let { navigator ->
            navigator.addDecorationListener("highlights", decorationListener)

            model.highlightDecorations
                .onEach { navigator.applyDecorations(it, "highlights") }
                .launchIn(viewScope)

            model.searchDecorations
                .onEach { navigator.applyDecorations(it, "search") }
                .launchIn(viewScope)
        }
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
        menuInflater.inflate(R.menu.menu_reader, menu)
        menu.findItem(R.id.drm).isVisible = model.publication.lcpLicense != null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toc -> {
                model.channel.send(ReaderViewModel.Event.OpenOutlineRequested)
                true
            }
            R.id.bookmark -> {
                model.insertBookmark(navigator.currentLocator.value)
                true
            }
            R.id.drm -> {
                model.channel.send(ReaderViewModel.Event.OpenDrmManagementRequested)
                true
            }
            else -> false
        }
    }

    fun go(locator: Locator, animated: Boolean) =
        navigator.go(locator, animated)

    // DecorableNavigator.Listener

    private val decorationListener by lazy { DecorationListener() }

    inner class DecorationListener : DecorableNavigator.Listener {
        override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
            val decoration = event.decoration
            // We stored the highlight's database ID in the `Decoration.extras` bundle, for
            // easy retrieval. You can store arbitrary information in the bundle.
            val id = decoration.extras.getLong("id")
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
                    showHighlightPopup(rect,
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
    private val highlightTints = mapOf<@IdRes Int, @ColorInt Int>(
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

    private fun showHighlightPopupWithStyle(style: Highlight.Style) = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        // Get the rect of the current selection to know where to position the highlight
        // popup.
        (navigator as? SelectableNavigator)?.currentSelection()?.rect?.let { selectionRect ->
            showHighlightPopup(selectionRect, style)
        }
    }

    private fun showHighlightPopup(rect: RectF, style: Highlight.Style, highlightId: Long? = null)
        = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            if (popupWindow?.isShowing == true) return@launchWhenResumed

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

        private fun selectHighlightTint(highlightId: Long? = null, style: Highlight.Style, @ColorInt tint: Int)
            = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                if (highlightId != null) {
                    model.updateHighlightStyle(highlightId, style, tint)
                } else {
                    (navigator as? SelectableNavigator)?.let { navigator ->
                        navigator.currentSelection()?.let { selection ->
                            model.addHighlight(locator = selection.locator, style = style, tint = tint)
                        }
                        navigator.clearSelection()
                    }
                }

                popupWindow?.dismiss()
                mode?.finish()
            }

    private fun showAnnotationPopup(highlightId: Long? = null) = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        val activity = activity ?: return@launchWhenResumed
        val view = layoutInflater.inflate(R.layout.popup_note, null, false)
        val note = view.findViewById<EditText>(R.id.note)
        val alert = AlertDialog.Builder(activity)
            .setView(view)
            .create()

        fun dismiss() {
            alert.dismiss()
            mode?.finish()
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(note.applicationWindowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }

        with(view) {
            val highlight = highlightId?.let { model.highlightById(it) }
            if (highlight != null) {
                note.setText(highlight.annotation)
                findViewById<View>(R.id.sidemark).setBackgroundColor(highlight.tint)
                findViewById<TextView>(R.id.select_text).text = highlight.locator.text.highlight

                findViewById<TextView>(R.id.positive).setOnClickListener {
                    val text = note.text.toString()
                    model.updateHighlightAnnotation(highlight.id, annotation = text)
                    dismiss()
                }
            } else {
                val tint = highlightTints.values.random()
                findViewById<View>(R.id.sidemark).setBackgroundColor(tint)
                val navigator = navigator as? SelectableNavigator ?: return@launchWhenResumed
                val selection = navigator.currentSelection() ?: return@launchWhenResumed
                navigator.clearSelection()
                findViewById<TextView>(R.id.select_text).text = selection.locator.text.highlight

                findViewById<TextView>(R.id.positive).setOnClickListener {
                    model.addHighlight(locator = selection.locator, style = Highlight.Style.HIGHLIGHT, tint = tint, annotation = note.text.toString())
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

/**
 * Decoration Style for a page margin icon.
 *
 * This is an example of a custom Decoration Style declaration.
 */
@Parcelize
@OptIn(ExperimentalDecorator::class)
data class DecorationStyleAnnotationMark(@ColorInt val tint: Int) : Decoration.Style
