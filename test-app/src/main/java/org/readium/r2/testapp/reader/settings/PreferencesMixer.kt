package org.readium.r2.testapp.reader.settings

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.settings.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.testapp.reader.NavigatorKind
import kotlin.reflect.KClass

@OptIn(ExperimentalReadiumApi::class)
class PreferencesMixer(
    private val application: Application,
    //private val pdfEngineProvider: PdfEngineProvider<*, *>
) {

    fun <P: Configurable.Preferences> mixPreferences(publication: P, navigator: P) =
        when ()


    fun getPreferences(bookId: Long, navigatorKind: NavigatorKind): Flow<Configurable.Preferences>? =
         when (navigatorKind) {
            NavigatorKind.EPUB_FIXEDLAYOUT ->
                getEpubFixedLayoutPreferences(bookId)
            NavigatorKind.EPUB_REFLOWABLE ->
                getEpubReflowablePreferences(bookId)
            NavigatorKind.PDF ->
                null //FIXME
            else ->
                null
        }

    suspend fun setPreferences(bookId: Long,preferences: Configurable.Preferences?) {
        when (preferences) {
            is EpubPreferences.FixedLayout ->
                setEpubFixedLayoutPreferences(bookId, preferences)
            is EpubPreferences.Reflowable ->
                setEpubReflowablePreferences(bookId, preferences)
            else -> {}
        }
    }

    private fun getEpubReflowablePreferences(bookId: Long): Flow<EpubPreferences.Reflowable> =
        combine(
            application.preferences.get<EpubPreferences.Reflowable>(bookId),
            application.preferences.get<EpubPreferences.Reflowable>(NavigatorKind.EPUB_REFLOWABLE)
        ) { pubPrefs, navPrefs ->
            when {
                pubPrefs != null && navPrefs != null ->
                    EpubPreferences.Reflowable.merge(pubPrefs, navPrefs)
                navPrefs != null ->
                    navPrefs
                else ->
                    EpubPreferences.Reflowable()
            }
        }

    private suspend fun setEpubReflowablePreferences(bookId: Long, preferences: EpubPreferences.Reflowable?) {
        if (preferences == null) {
            application.preferences.set<EpubPreferences.Reflowable>(bookId, null)
            application.preferences.set<EpubPreferences.Reflowable>(NavigatorKind.EPUB_REFLOWABLE, null)
            return
        }

        val pubPrefs = preferences.filterPublicationPreferences()
        val navPrefs = preferences.filterNavigatorPreferences()
        application.preferences.set(bookId, pubPrefs)
        application.preferences.set(NavigatorKind.EPUB_REFLOWABLE, navPrefs)
    }

    private fun getEpubFixedLayoutPreferences(bookId: Long): Flow<EpubPreferences.FixedLayout> =
        combine(
            application.preferences.get<EpubPreferences.FixedLayout>(bookId),
            application.preferences.get<EpubPreferences.FixedLayout>(NavigatorKind.EPUB_FIXEDLAYOUT)
        ) { pubPrefs, navPrefs ->
            when {
                pubPrefs != null && navPrefs != null ->
                    EpubPreferences.FixedLayout.merge(pubPrefs, navPrefs)
                navPrefs != null ->
                    navPrefs
                else ->
                    EpubPreferences.FixedLayout()
            }
        }

    private suspend fun setEpubFixedLayoutPreferences(bookId: Long, preferences: EpubPreferences.FixedLayout?) {
        if (preferences == null) {
            application.preferences.set<EpubPreferences.FixedLayout>(bookId, null)
            application.preferences.set<EpubPreferences.FixedLayout>(NavigatorKind.EPUB_FIXEDLAYOUT, null)
            return
        }

        val pubPrefs = preferences.filterPublicationPreferences()
        val navPrefs = preferences.filterNavigatorPreferences()
        application.preferences.set(bookId, pubPrefs)
        application.preferences.set(NavigatorKind.EPUB_FIXEDLAYOUT, navPrefs)
    }
}