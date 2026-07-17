package org.readium.r2.shared.util.zip.jvm;

/**
 * An {@code AsynchronousCloseException} is thrown when the underlying channel
 * for an I/O operation is closed by another thread.
 */
public class AsynchronousCloseException extends ClosedChannelException {
    private static final long serialVersionUID = 6891178312432313966L;
    /**
     * Constructs an {@code AsynchronousCloseException}.
     */
    public AsynchronousCloseException() {
    }
}
