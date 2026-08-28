/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp.license.container

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal fun ZipFile.addOrReplaceEntry(
    name: String,
    inputStream: InputStream,
    dest: File,
) {
    addOrReplaceEntry(name, inputStream, FileOutputStream(dest))
}

internal fun ZipFile.addOrReplaceEntry(
    name: String,
    inputStream: InputStream,
    dest: OutputStream,
) {
    val outZip = ZipOutputStream(dest)
    var entryAdded = false

    val newEntry = ZipEntry(name)
    newEntry.method = ZipEntry.DEFLATED
    getEntry(name)?.let { originalEntry ->
        newEntry.extra = originalEntry.extra
        newEntry.comment = originalEntry.comment
    }

    for (entry in entries()) {
        if (entry.name == name) {
            addEntry(newEntry, inputStream, outZip)
            entryAdded = true
        } else {
            copyEntry(entry.copy(), this, outZip)
        }
    }

    if (!entryAdded) {
        addEntry(newEntry, inputStream, outZip)
    }

    outZip.finish()
    outZip.close()
}

private fun ZipEntry.copy(): ZipEntry {
    val copy = ZipEntry(name)
    if (crc != -1L) {
        copy.crc = crc
    }
    if (method != -1) {
        copy.method = method
    }
    if (size >= 0) {
        copy.size = size
    }
    if (extra != null) {
        copy.extra = extra
    }
    copy.comment = comment
    copy.time = time
    return copy
}

/**
 * If STORED method is used, entry must contain CRC and size.
 */
private fun addEntry(
    entry: ZipEntry,
    source: InputStream,
    outStream: ZipOutputStream,
) {
    outStream.putNextEntry(entry)
    source.copyTo(outStream)
    outStream.closeEntry()
}

private fun copyEntry(
    entry: ZipEntry,
    srcZip: ZipFile,
    outStream: ZipOutputStream,
) {
    addEntry(entry, srcZip.getInputStream(entry), outStream)
}
