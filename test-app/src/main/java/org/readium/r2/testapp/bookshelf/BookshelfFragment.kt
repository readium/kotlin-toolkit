/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.bookshelf

import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentBookshelfBinding
import org.readium.r2.testapp.domain.model.Book
import org.readium.r2.testapp.opds.GridAutoFitLayoutManager
import org.readium.r2.testapp.reader.ReaderActivityContract
import org.readium.r2.testapp.utils.viewLifecycle

class BookshelfFragment : Fragment() {

    private val bookshelfViewModel: BookshelfViewModel by activityViewModels()
    private lateinit var bookshelfAdapter: BookshelfAdapter
    private lateinit var documentPickerLauncher: ActivityResultLauncher<String>
    private lateinit var readerLauncher: ActivityResultLauncher<ReaderActivityContract.Arguments>
    private var binding: FragmentBookshelfBinding by viewLifecycle()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookshelfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bookshelfViewModel.channel.receive(viewLifecycleOwner) { handleEvent(it) }

        bookshelfAdapter = BookshelfAdapter(
            onBookClick = { book -> book.id?.let {  bookshelfViewModel.openBook(it) } },
            onBookLongClick = { book -> confirmDeleteBook(book) })

        documentPickerLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    binding.bookshelfProgressBar.visibility = View.VISIBLE
                    bookshelfViewModel.importPublicationFromUri(it)
                }
            }

        readerLauncher =
            registerForActivityResult(ReaderActivityContract()) { input ->
                input?.let { tryOrLog { bookshelfViewModel.closeBook(input.bookId) } }
            }

        binding.bookshelfBookList.apply {
            setHasFixedSize(true)
            layoutManager = GridAutoFitLayoutManager(requireContext(), 120)
            adapter = bookshelfAdapter
            addItemDecoration(
                VerticalSpaceItemDecoration(
                    10
                )
            )
        }

        bookshelfViewModel.books.observe(viewLifecycleOwner) {
            bookshelfAdapter.submitList(it)
        }

        // FIXME embedded dialogs like this are ugly
        binding.bookshelfAddBookFab.setOnClickListener {
            var selected = 0
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.add_book))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    if (selected == 0) {
                        documentPickerLauncher.launch("*/*")
                    } else {
                        val urlEditText = EditText(requireContext())
                        val urlDialog = MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.add_book))
                            .setMessage(R.string.enter_url)
                            .setView(urlEditText)
                            .setNegativeButton(R.string.cancel) { dialog, _ ->
                                dialog.cancel()
                            }
                            .setPositiveButton(getString(R.string.ok), null)
                            .show()
                        urlDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            if (TextUtils.isEmpty(urlEditText.text)) {
                                urlEditText.error = getString(R.string.invalid_url)
                            } else if (!URLUtil.isValidUrl(urlEditText.text.toString())) {
                                urlEditText.error = getString(R.string.invalid_url)
                            } else {
                                val url = urlEditText.text.toString()
                                val uri = Uri.parse(url)
                                binding.bookshelfProgressBar.visibility = View.VISIBLE
                                bookshelfViewModel.importPublicationFromUri(uri)
                                urlDialog.dismiss()
                            }
                        }
                    }
                }
                .setSingleChoiceItems(R.array.documentSelectorArray, 0) { _, which ->
                    selected = which
                }
                .show()
        }
    }

    private fun handleEvent(event: BookshelfViewModel.Event) {
        val message =
            when (event) {
                is BookshelfViewModel.Event.ImportPublicationFailed -> {
                    "Error: " + event.errorMessage
                }
                is BookshelfViewModel.Event.UnableToMovePublication ->
                    getString(R.string.unable_to_move_pub)
                is BookshelfViewModel.Event.ImportPublicationSuccess -> getString(R.string.import_publication_success)
                is BookshelfViewModel.Event.ImportDatabaseFailed ->
                    getString(R.string.unable_add_pub_database)
                is BookshelfViewModel.Event.OpenBookError -> {
                    val detail = event.errorMessage
                        ?: "Unable to open publication. An unexpected error occurred."
                    "Error: $detail"
                }
                is BookshelfViewModel.Event.LaunchReader -> {
                    readerLauncher.launch(event.arguments)
                    null
                }
            }
        binding.bookshelfProgressBar.visibility = View.GONE
        message?.let {
            Snackbar.make(
                requireView(),
                it,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) :
        RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect, view: View, parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.bottom = verticalSpaceHeight
        }
    }

    private fun deleteBook(book: Book) {
        bookshelfViewModel.deleteBook(book)
    }

    private fun confirmDeleteBook(book: Book) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_delete_book_title))
            .setMessage(getString(R.string.confirm_delete_book_text))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                deleteBook(book)
                dialog.dismiss()
            }
            .show()
    }
}
