/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Ported to Kotlin Multiplatform for Readium from the vendored Commons Compress subset
// (Java sources: `UnparseableExtraFieldBehavior.java` and `ExtraFieldParsingBehavior.java`,
// phase 05b).

package org.readium.r2.shared.util.zip.compress.archivers.zip

/**
 * Handles extra field data that doesn't follow the recommended pattern.
 */
internal interface UnparseableExtraFieldBehavior {

    /**
     * Decides what to do with extra field data that doesn't follow the recommended pattern.
     *
     * @param data the array of extra field data
     * @param off offset into data where the unparseable data starts
     * @param len the length of unparseable data
     * @param local whether the extra field data stems from the local file header
     * @param claimedLength length of the extra field claimed by the third and forth byte if
     * parsing it as an extra field
     * @return null if the data should be ignored or an extra field implementation that represents
     * the data
     * @throws ZipException if the data is considered invalid.
     */
    fun onUnparseableExtraField(
        data: ByteArray,
        off: Int,
        len: Int,
        local: Boolean,
        claimedLength: Int,
    ): ZipExtraField?
}

/**
 * Controls details of parsing zip extra fields.
 */
internal interface ExtraFieldParsingBehavior : UnparseableExtraFieldBehavior {

    /**
     * Creates an instance of [ZipExtraField] for the given id.
     *
     * @throws ZipException if an error occurs.
     */
    fun createExtraField(headerId: ZipShort): ZipExtraField

    /**
     * Fills in the extra field data for a single extra field.
     *
     * @param field the extra field instance to fill
     * @param data the array of extra field data
     * @param off offset into data where this field's data starts
     * @param len the length of this field's data
     * @param local whether the extra field data stems from the local file header
     * @return the filled field. Usually this is the same as [field] but it could be a replacement
     * extra field if the instance could not be filled
     * @throws ZipException if the data is malformed.
     */
    fun fill(field: ZipExtraField, data: ByteArray, off: Int, len: Int, local: Boolean): ZipExtraField
}
