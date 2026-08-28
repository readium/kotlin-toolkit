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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.readium.r2.shared.util.zip.compress.utils.InputStreamStatistics;

/**
 * Helper class to provide statistics
 *
 * @since 1.17
 */
/* package */ class InflaterInputStreamWithStatistics extends InflaterInputStream
    implements InputStreamStatistics {
    private long compressedCount;
    private long uncompressedCount;

    public InflaterInputStreamWithStatistics(final InputStream in) {
        super(in);
    }

    public InflaterInputStreamWithStatistics(final InputStream in, final Inflater inf) {
        super(in, inf);
    }

    public InflaterInputStreamWithStatistics(final InputStream in, final Inflater inf, final int size) {
        super(in, inf, size);
    }

    @Override
    protected void fill() throws IOException {
        super.fill();
        compressedCount += inf.getRemaining();
    }

    @Override
    public long getCompressedCount() {
        return compressedCount;
    }

    @Override
    public long getUncompressedCount() {
        return uncompressedCount;
    }

    @Override
    public int read() throws IOException {
        final int b = super.read();
        if (b > -1) {
            uncompressedCount++;
        }
        return b;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int bytes = super.read(b, off, len);
        if (bytes > -1) {
            uncompressedCount += bytes;
        }
        return bytes;
    }
}
