/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.testapp.search

import androidx.annotation.StringRes
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.services.search.SearchError
import org.readium.r2.shared.util.Error
import org.readium.r2.testapp.R
import org.readium.r2.testapp.utils.UserError

sealed class SearchUserError(
    override val content: UserError.Content,
    override val cause: UserError? = null
) : UserError {
    constructor(@StringRes userMessageId: Int) :
        this(UserError.Content(userMessageId), null)
    object PublicationNotSearchable :
        SearchUserError(R.string.search_error_not_searchable)

    class Reading(val error: Error) :
        SearchUserError(R.string.search_error_other)

    class Engine(val error: Error) :
        SearchUserError(R.string.search_error_other)

    companion object {

        @OptIn(ExperimentalReadiumApi::class)
        operator fun invoke(error: SearchError): SearchUserError =
            when (error) {
                is SearchError.Reading ->
                    Reading(error)
                is SearchError.Engine ->
                    Engine(error)
            }
    }
}
