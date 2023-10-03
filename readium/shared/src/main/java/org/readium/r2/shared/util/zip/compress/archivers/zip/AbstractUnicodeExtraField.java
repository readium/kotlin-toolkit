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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.zip.CRC32;
import java.util.zip.ZipException;

/**
 * A common base class for Unicode extra information extra fields.
 * @NotThreadSafe
 */
public abstract class AbstractUnicodeExtraField implements ZipExtraField {
    private long nameCRC32;
    private byte[] unicodeName;
    private byte[] data;

    protected AbstractUnicodeExtraField() {
    }

    /**
     * Assemble as unicode extension from the name/comment and
     * encoding of the original zip entry.
     *
     * @param text The file name or comment.
     * @param bytes The encoded of the file name or comment in the zip
     * file.
     */
    protected AbstractUnicodeExtraField(final String text, final byte[] bytes) {
        this(text, bytes, 0, bytes.length);
    }

    /**
     * Assemble as unicode extension from the name/comment and
     * encoding of the original zip entry.
     *
     * @param text The file name or comment.
     * @param bytes The encoded of the file name or comment in the zip
     * file.
     * @param off The offset of the encoded file name or comment in
     * {@code bytes}.
     * @param len The length of the encoded file name or comment in
     * {@code bytes}.
     */
    protected AbstractUnicodeExtraField(final String text, final byte[] bytes, final int off, final int len) {
        final CRC32 crc32 = new CRC32();
        crc32.update(bytes, off, len);
        nameCRC32 = crc32.getValue();

        unicodeName = text.getBytes(UTF_8);
    }

    private void assembleData() {
        if (unicodeName == null) {
            return;
        }

        data = new byte[5 + unicodeName.length];
        // version 1
        data[0] = 0x01;
        System.arraycopy(ZipLong.getBytes(nameCRC32), 0, data, 1, 4);
        System.arraycopy(unicodeName, 0, data, 5, unicodeName.length);
    }

    @Override
    public byte[] getCentralDirectoryData() {
        if (data == null) {
            this.assembleData();
        }
        byte[] b = null;
        if (data != null) {
            b = new byte[data.length];
            System.arraycopy(data, 0, b, 0, b.length);
        }
        return b;
    }

    @Override
    public ZipShort getCentralDirectoryLength() {
        if (data == null) {
            assembleData();
        }
        return new ZipShort(data != null ? data.length : 0);
    }

    @Override
    public byte[] getLocalFileDataData() {
        return getCentralDirectoryData();
    }

    @Override
    public ZipShort getLocalFileDataLength() {
        return getCentralDirectoryLength();
    }

    /**
     * @return The CRC32 checksum of the file name or comment as
     *         encoded in the central directory of the zip file.
     */
    public long getNameCRC32() {
        return nameCRC32;
    }

    /**
     * @return The UTF-8 encoded name.
     */
    public byte[] getUnicodeName() {
        byte[] b = null;
        if (unicodeName != null) {
            b = new byte[unicodeName.length];
            System.arraycopy(unicodeName, 0, b, 0, b.length);
        }
        return b;
    }

    /**
     * Doesn't do anything special since this class always uses the
     * same data in central directory and local file data.
     */
    @Override
    public void parseFromCentralDirectoryData(final byte[] buffer, final int offset,
                                              final int length)
        throws ZipException {
        parseFromLocalFileData(buffer, offset, length);
    }

    @Override
    public void parseFromLocalFileData(final byte[] buffer, final int offset, final int length)
        throws ZipException {

        if (length < 5) {
            throw new ZipException("UniCode path extra data must have at least 5 bytes.");
        }

        final int version = buffer[offset];

        if (version != 0x01) {
            throw new ZipException("Unsupported version [" + version
                                   + "] for UniCode path extra data.");
        }

        nameCRC32 = ZipLong.getValue(buffer, offset + 1);
        unicodeName = new byte[length - 5];
        System.arraycopy(buffer, offset + 5, unicodeName, 0, length - 5);
        data = null;
    }

    /**
     * @param nameCRC32 The CRC32 checksum of the file name as encoded
     *         in the central directory of the zip file to set.
     */
    public void setNameCRC32(final long nameCRC32) {
        this.nameCRC32 = nameCRC32;
        data = null;
    }

    /**
     * @param unicodeName The UTF-8 encoded name to set.
     */
    public void setUnicodeName(final byte[] unicodeName) {
        if (unicodeName != null) {
            this.unicodeName = new byte[unicodeName.length];
            System.arraycopy(unicodeName, 0, this.unicodeName, 0,
                             unicodeName.length);
        } else {
            this.unicodeName = null;
        }
        data = null;
    }
}
