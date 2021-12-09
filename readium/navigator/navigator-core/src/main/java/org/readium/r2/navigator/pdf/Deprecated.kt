package org.readium.r2.navigator.pdf

import androidx.fragment.app.Fragment

@Deprecated(
    message = "Moved to a new module readium-pdfium-navigator",
    replaceWith = ReplaceWith("org.readium.adapters.pdfium.navigator.PdfNavigatorFragment"),
    level = DeprecationLevel.ERROR
)
class PdfNavigatorFragment : Fragment()

@Deprecated(
    message = "Use `PdfNavigatorFragment wrapped in your own activity",
    level = DeprecationLevel.ERROR
)
class R2PdfActivity
