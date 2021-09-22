/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.bookshelf

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import org.json.JSONObject
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.publication.Locator
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentBookshelfBinding
import org.readium.r2.testapp.domain.model.Book
import org.readium.r2.testapp.opds.GridAutoFitLayoutManager
import org.readium.r2.testapp.reader.ReaderContract
import java.io.File


class BookshelfFragment : Fragment() {

    private val bookshelfViewModel: BookshelfViewModel by viewModels()
    private lateinit var bookshelfAdapter: BookshelfAdapter
    private lateinit var documentPickerLauncher: ActivityResultLauncher<String>
    private lateinit var readerLauncher: ActivityResultLauncher<ReaderContract.Input>
    private var permissionAsked: Boolean = false
    private var _binding: FragmentBookshelfBinding? = null
    private val binding get() = _binding!!

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                importBooks()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        bookshelfViewModel.channel.receive(this) { handleEvent(it) }
        _binding = FragmentBookshelfBinding.inflate(
            inflater, container, false
        )
        binding.viewModel = bookshelfViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bookshelfAdapter = BookshelfAdapter(onBookClick = { book -> openBook(book.id) },
            onBookLongClick = { book -> confirmDeleteBook(book) })

        documentPickerLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    bookshelfViewModel.importPublicationFromUri(it)
                }
            }

        readerLauncher =
            registerForActivityResult(ReaderContract()) { pubData: ReaderContract.Output? ->
                tryOrLog { pubData?.publication?.close() }
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

        bookshelfViewModel.books.observe(viewLifecycleOwner, {
            bookshelfAdapter.submitList(it)
        })

        importBooks()

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
                                bookshelfViewModel.importPublicationFromUri(uri, url)
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
                is BookshelfViewModel.Event.UnableToMovePublication -> getString(R.string.unable_to_move_pub)
                is BookshelfViewModel.Event.ImportPublicationSuccess -> getString(R.string.import_publication_success)
                is BookshelfViewModel.Event.ImportDatabaseFailed -> getString(R.string.unable_add_pub_database)
                is BookshelfViewModel.Event.OpenBookError -> {
                    "Error: " + event.errorMessage
                }
            }
        Snackbar.make(
            requireView(),
            message,
            Snackbar.LENGTH_LONG
        ).show()
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

    private fun requestStoragePermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(
                this.requireView(),
                R.string.permission_external_new_explanation,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.permission_retry) {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                .show()
        } else {
            // FIXME this is an ugly hack for when user has said don't ask again
            if (permissionAsked) {
                Snackbar.make(
                    this.requireView(),
                    R.string.permission_external_new_explanation,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.action_settings) {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            addCategory(Intent.CATEGORY_DEFAULT)
                            data = Uri.parse("package:${view?.context?.packageName}")
                        }.run(::startActivity)
                    }
                    .setActionTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.snackbar_text_color
                        )
                    )
                    .show()
            } else {
                permissionAsked = true
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun importBooks() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            bookshelfViewModel.copySamplesFromAssetsToStorage()
        } else requestStoragePermission()

    }

    private fun deleteBook(book: Book) {
        bookshelfViewModel.deleteBook(book)
    }

    private fun openBook(bookId: Long?) {
        bookId ?: return

        bookshelfViewModel.openBook(requireContext(), bookId) { book, asset, publication, url ->
            readerLauncher.launch(ReaderContract.Input(
                mediaType = asset.mediaType(),
                publication = publication,
                bookId = bookId,
                initialLocator = book.progression?.let { Locator.fromJSON(JSONObject(it)) },
                baseUrl = url
            ))
        }
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

@BindingAdapter("coverImage")
fun loadImage(view: ImageView, bookId: Long?) {
    val coverImageFile = File("${view.context?.filesDir?.path}/covers/${bookId}.png")
    Picasso.with(view.context)
        .load(coverImageFile)
        .placeholder(R.drawable.cover)
        .into(view)
}