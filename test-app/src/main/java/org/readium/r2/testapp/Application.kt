/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.material.color.DynamicColors
import java.io.File
import java.util.Properties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import org.readium.r2.testapp.BuildConfig.DEBUG
import org.readium.r2.testapp.data.BookRepository
import org.readium.r2.testapp.data.DownloadRepository
import org.readium.r2.testapp.data.db.AppDatabase
import org.readium.r2.testapp.data.model.Download
import org.readium.r2.testapp.domain.Bookshelf
import org.readium.r2.testapp.domain.CoverStorage
import org.readium.r2.testapp.domain.LcpPublicationRetriever
import org.readium.r2.testapp.domain.LocalPublicationRetriever
import org.readium.r2.testapp.domain.OpdsPublicationRetriever
import org.readium.r2.testapp.domain.PublicationRetriever
import org.readium.r2.testapp.reader.ReaderRepository
import timber.log.Timber

class Application : android.app.Application() {

    lateinit var readium: Readium
        private set

    lateinit var storageDir: File

    lateinit var bookRepository: BookRepository
        private set

    lateinit var bookshelf: Bookshelf
        private set

    lateinit var readerRepository: Deferred<ReaderRepository>
        private set

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val Context.navigatorPreferences: DataStore<Preferences>
        by preferencesDataStore(name = "navigator-preferences")

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        if (DEBUG) Timber.plant(Timber.DebugTree())

        readium = Readium(this)

        storageDir = computeStorageDir()

        val database = AppDatabase.getDatabase(this)

        bookRepository = BookRepository(database.booksDao())

        bookshelf =
            Bookshelf(
                bookRepository,
                CoverStorage(storageDir, httpClient = readium.httpClient),
                readium.publicationFactory,
                readium.assetRetriever,
                readium.protectionRetriever,
                createPublicationRetriever = { listener ->
                    PublicationRetriever(
                        listener = listener,
                        createLocalPublicationRetriever = { localListener ->
                            LocalPublicationRetriever(
                                listener = localListener,
                                context = applicationContext,
                                storageDir = storageDir,
                                assetRetriever = readium.assetRetriever,
                                formatRegistry = readium.formatRegistry,
                                createLcpPublicationRetriever = { lcpListener ->
                                    readium.lcpService.getOrNull()?.publicationRetriever()
                                        ?.let { retriever ->
                                            LcpPublicationRetriever(
                                                listener = lcpListener,
                                                downloadRepository = DownloadRepository(
                                                    Download.Type.LCP,
                                                    database.downloadsDao()
                                                ),
                                                lcpPublicationRetriever = retriever
                                            )
                                        }
                                }
                            )
                        },
                        createOpdsPublicationRetriever = { opdsListener ->
                            OpdsPublicationRetriever(
                                listener = opdsListener,
                                downloadManager = readium.downloadManager,
                                downloadRepository = DownloadRepository(
                                    Download.Type.OPDS,
                                    database.downloadsDao()
                                )
                            )
                        }
                    )
                }
            )

        readerRepository =
            coroutineScope.async {
                ReaderRepository(
                    this@Application,
                    readium,
                    bookRepository,
                    navigatorPreferences
                )
            }
    }

    private fun computeStorageDir(): File {
        val properties = Properties()
        val inputStream = assets.open("configs/config.properties")
        properties.load(inputStream)
        val useExternalFileDir =
            properties.getProperty("useExternalFileDir", "false")!!.toBoolean()

        return File(
            if (useExternalFileDir) {
                getExternalFilesDir(null)?.path + "/"
            } else {
                filesDir?.path + "/"
            }
        )
    }
}
