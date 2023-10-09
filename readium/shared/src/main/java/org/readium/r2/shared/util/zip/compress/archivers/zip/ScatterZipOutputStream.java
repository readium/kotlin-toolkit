/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.readium.r2.shared.util.zip.compress.archivers.zip;

import org.readium.r2.shared.util.zip.compress.parallel.ScatterGatherBackingStore;
import org.readium.r2.shared.util.zip.compress.utils.BoundedInputStream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A zip output stream that is optimized for multi-threaded scatter/gather construction of zip files.
 * <p>
 * The internal data format of the entries used by this class are entirely private to this class
 * and are not part of any public api whatsoever.
 * </p>
 * <p>It is possible to extend this class to support different kinds of backing storage, the default
 * implementation only supports file-based backing.
 * </p>
 * Thread safety: This class supports multiple threads. But the "writeTo" method must be called
 * by the thread that originally created the {@link ZipArchiveEntry}.
 *
 * @since 1.10
 */
public class ScatterZipOutputStream implements Closeable {
    private static class CompressedEntry {
        final ZipArchiveEntryRequest zipArchiveEntryRequest;
        final long crc;
        final long compressedSize;
        final long size;

        public CompressedEntry(final ZipArchiveEntryRequest zipArchiveEntryRequest, final long crc, final long compressedSize, final long size) {
            this.zipArchiveEntryRequest = zipArchiveEntryRequest;
            this.crc = crc;
            this.compressedSize = compressedSize;
            this.size = size;
        }

        /**
         * Update the original {@link ZipArchiveEntry} with sizes/crc
         * Do not use this methods from threads that did not create the instance itself !
         * @return the zipArchiveEntry that is basis for this request
         */

        public ZipArchiveEntry transferToArchiveEntry(){
            final ZipArchiveEntry entry = zipArchiveEntryRequest.getZipArchiveEntry();
            entry.setCompressedSize(compressedSize);
            entry.setSize(size);
            entry.setCrc(crc);
            entry.setMethod(zipArchiveEntryRequest.getMethod());
            return entry;
        }
    }
    public static class ZipEntryWriter implements Closeable {
        private final Iterator<CompressedEntry> itemsIterator;
        private final InputStream itemsIteratorData;

        public ZipEntryWriter(final ScatterZipOutputStream scatter) throws IOException {
            scatter.backingStore.closeForWriting();
            itemsIterator = scatter.items.iterator();
            itemsIteratorData = scatter.backingStore.getInputStream();
        }

        @Override
        public void close() throws IOException {
            if (itemsIteratorData != null) {
                itemsIteratorData.close();
            }
        }

        public void writeNextZipEntry(final ZipArchiveOutputStream target) throws IOException {
            final CompressedEntry compressedEntry = itemsIterator.next();
            try (final BoundedInputStream rawStream = new BoundedInputStream(itemsIteratorData, compressedEntry.compressedSize)) {
                target.addRawArchiveEntry(compressedEntry.transferToArchiveEntry(), rawStream);
            }
        }
    }

    private final Queue<CompressedEntry> items = new ConcurrentLinkedQueue<>();

    private final ScatterGatherBackingStore backingStore;

    private final StreamCompressor streamCompressor;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    private ZipEntryWriter zipEntryWriter;

    public ScatterZipOutputStream(final ScatterGatherBackingStore backingStore,
                                  final StreamCompressor streamCompressor) {
        this.backingStore = backingStore;
        this.streamCompressor = streamCompressor;
    }

    /**
     * Add an archive entry to this scatter stream.
     *
     * @param zipArchiveEntryRequest The entry to write.
     * @throws IOException    If writing fails
     */
    public void addArchiveEntry(final ZipArchiveEntryRequest zipArchiveEntryRequest) throws IOException {
        try (final InputStream payloadStream = zipArchiveEntryRequest.getPayloadStream()) {
            streamCompressor.deflate(payloadStream, zipArchiveEntryRequest.getMethod());
        }
        items.add(new CompressedEntry(zipArchiveEntryRequest, streamCompressor.getCrc32(),
                                      streamCompressor.getBytesWrittenForLastEntry(), streamCompressor.getBytesRead()));
    }

    /**
     * Closes this stream, freeing all resources involved in the creation of this stream.
     * @throws IOException If closing fails
     */
    @Override
    public void close() throws IOException {
        if (!isClosed.compareAndSet(false, true)) {
            return;
        }
        try {
            if (zipEntryWriter != null) {
                zipEntryWriter.close();
            }
            backingStore.close();
        } finally {
            streamCompressor.close();
        }
    }

    /**
     * Write the contents of this scatter stream to a target archive.
     *
     * @param target The archive to receive the contents of this {@link ScatterZipOutputStream}.
     * @throws IOException If writing fails
     * @see #zipEntryWriter()
     */
    public void writeTo(final ZipArchiveOutputStream target) throws IOException {
        backingStore.closeForWriting();
        try (final InputStream data = backingStore.getInputStream()) {
            for (final CompressedEntry compressedEntry : items) {
                try (final BoundedInputStream rawStream = new BoundedInputStream(data,
                        compressedEntry.compressedSize)) {
                    target.addRawArchiveEntry(compressedEntry.transferToArchiveEntry(), rawStream);
                }
            }
        }
    }

    /**
     * Get a zip entry writer for this scatter stream.
     * @throws IOException If getting scatter stream input stream
     * @return the ZipEntryWriter created on first call of the method
     */
    public ZipEntryWriter zipEntryWriter() throws IOException {
        if (zipEntryWriter == null) {
            zipEntryWriter = new ZipEntryWriter(this);
        }
        return zipEntryWriter;
    }
}
