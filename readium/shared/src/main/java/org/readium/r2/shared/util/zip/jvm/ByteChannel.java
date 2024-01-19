package org.readium.r2.shared.util.zip.jvm;
/**
 * A ByteChannel is both readable and writable.
 * <p>
 * The methods for the byte channel are precisely those defined by readable and
 * writable byte channels.
 *
 * @see ReadableByteChannel
 * @see WritableByteChannel
 */
public interface ByteChannel extends ReadableByteChannel, WritableByteChannel {
    // No methods defined.
}
