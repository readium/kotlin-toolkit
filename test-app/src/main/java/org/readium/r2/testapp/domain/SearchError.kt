package org.readium.r2.testapp.domain

import androidx.annotation.StringRes
import org.readium.r2.shared.UserException
import org.readium.r2.shared.util.Error
import org.readium.r2.testapp.R

sealed class SearchError(@StringRes userMessageId: Int) : UserException(userMessageId) {

    object PublicationNotSearchable :
        SearchError(R.string.search_error_not_searchable)

    class BadQuery(val error: Error) :
        SearchError(R.string.search_error_not_searchable)

    class ResourceError(val error: Error) :
        SearchError(R.string.search_error_other)

    class NetworkError(val error: Error) :
        SearchError(R.string.search_error_other)

    object Cancelled :
        SearchError(R.string.search_error_cancelled)

    class Other(val error: Error) :
        SearchError(R.string.search_error_other)
}
