package org.readium.r2.shared.util.zip.jvm;

import java.io.IOException;
import java.nio.ByteBuffer;
/**
 * A {@code WritableByteChannel} is a type of {@link Channel} that can write
 * bytes.
 * <p>
 * Write operations are synchronous on a {@code WritableByteChannel}, that is,
 * if a write is already in progress on the channel then subsequent writes will
 * block until the first write completes. It is undefined whether non-write
 * operations will block.
 */
public interface WritableByteChannel extends Channel {
    /**
     * Writes bytes from the given buffer to the channel.
     * <p>
     * The maximum number of bytes that will be written is the
     * <code>remaining()</code> number of bytes in the buffer when the method
     * invoked. The bytes will be written from the buffer starting at the
     * buffer's <code>position</code>.
     * <p>
     * The call may block if other threads are also attempting to write on the
     * same channel.
     * <p>
     * Upon completion, the buffer's <code>position()</code> is updated to the
     * end of the bytes that were written. The buffer's <code>limit()</code>
     * is unmodified.
     *
     * @param buffer
     *            the byte buffer containing the bytes to be written.
     * @return the number of bytes actually written.
     * @throws NonWritableChannelException
     *             if the channel was not opened for writing.
     * @throws ClosedChannelException
     *             if the channel was already closed.
     * @throws AsynchronousCloseException
     *             if another thread closes the channel during the write.
     * @throws ClosedByInterruptException
     *             if another thread interrupt the calling thread during the
     *             write.
     * @throws IOException
     *             another IO exception occurs, details are in the message.
     */
    public int write(ByteBuffer buffer) throws IOException;
}
