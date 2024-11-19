/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.navigator

import android.os.Bundle
import androidx.fragment.app.Fragment
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isRestricted

public abstract class NavigatorFragment internal constructor(
    protected val publication: Publication,
) : Fragment(), Navigator {

    override fun onCreate(savedInstanceState: Bundle?) {
        require(!publication.isRestricted) { "The provided publication is restricted. Check that any DRM was properly unlocked using a Content Protection." }

        super.onCreate(savedInstanceState)
    }
}
