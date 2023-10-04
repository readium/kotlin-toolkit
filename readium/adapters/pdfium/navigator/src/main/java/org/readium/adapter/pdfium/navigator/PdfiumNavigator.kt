/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.adapter.pdfium.navigator

import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi

@ExperimentalReadiumApi
public typealias PdfiumNavigatorFragment = PdfNavigatorFragment<PdfiumSettings, PdfiumPreferences>

@ExperimentalReadiumApi
public typealias PdfiumNavigatorFactory = PdfNavigatorFactory<PdfiumSettings, PdfiumPreferences, PdfiumPreferencesEditor>
