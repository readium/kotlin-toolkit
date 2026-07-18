/*
 * Copyright 2026 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.tokenizer

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language
import platform.CoreFoundation.CFLocaleCreate
import platform.CoreFoundation.CFLocaleRef
import platform.CoreFoundation.CFRange
import platform.CoreFoundation.CFRangeMake
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFStringTokenizerAdvanceToNextToken
import platform.CoreFoundation.CFStringTokenizerCreate
import platform.CoreFoundation.CFStringTokenizerGetCurrentTokenRange
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFStringTokenizerTokenNone
import platform.CoreFoundation.kCFStringTokenizerUnitSentence
import platform.CoreFoundation.kCFStringTokenizerUnitWordBoundary
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSString
import platform.Foundation.create

/**
 * A default cluster [TextTokenizer] backed by `CFStringTokenizer` on iOS.
 */
@ExperimentalReadiumApi
public actual class DefaultTextContentTokenizer public actual constructor(
    private val unit: TextUnit,
    private val language: Language?,
) : Tokenizer<String, IntRange> {

    init {
        require(unit != TextUnit.Paragraph) {
            "DefaultTextContentTokenizer does not handle TextContentUnit.Paragraph"
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    @Suppress("UNCHECKED_CAST")
    actual override fun tokenize(data: String): List<IntRange> {
        if (data.isEmpty()) {
            return emptyList()
        }

        val cfString = CFBridgingRetain(NSString.create(string = data)) as CFStringRef?
            ?: return emptyList()

        try {
            val cfLocale = language
                ?.let {
                    val identifier = CFBridgingRetain(NSString.create(string = it.code.replace('-', '_'))) as CFStringRef?
                    try {
                        CFLocaleCreate(kCFAllocatorDefault, identifier)
                    } finally {
                        identifier?.let { id -> CFRelease(id) }
                    }
                }

            try {
                return tokenize(data, cfString, cfLocale)
            } finally {
                cfLocale?.let { CFRelease(it) }
            }
        } finally {
            CFRelease(cfString)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun tokenize(data: String, cfString: CFStringRef, cfLocale: CFLocaleRef?): List<IntRange> {
        val tokenizerUnit = when (unit) {
            TextUnit.Word -> kCFStringTokenizerUnitWordBoundary
            TextUnit.Sentence -> kCFStringTokenizerUnitSentence
            TextUnit.Paragraph -> throw IllegalArgumentException(
                "DefaultTextContentTokenizer does not handle TextContentUnit.Paragraph"
            )
        }

        val tokenizer = CFStringTokenizerCreate(
            kCFAllocatorDefault,
            cfString,
            CFRangeMake(0, data.length.toLong()),
            tokenizerUnit,
            cfLocale
        ) ?: return emptyList()

        try {
            return buildList {
                while (CFStringTokenizerAdvanceToNextToken(tokenizer) != kCFStringTokenizerTokenNone) {
                    val range: IntRange? = CFStringTokenizerGetCurrentTokenRange(tokenizer)
                        .useContents<CFRange, IntRange?> {
                            data.sanitizeRange(location.toInt(), (location + length).toInt())
                        }
                    range?.let { add(it) }
                }
            }
        } finally {
            CFRelease(tokenizer)
        }
    }
}
