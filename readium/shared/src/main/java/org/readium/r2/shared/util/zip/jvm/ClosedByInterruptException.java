package org.readium.r2.shared.util.zip.jvm;

/**
 * A {@code ClosedByInterruptException} is thrown when a thread is interrupted
 * in a blocking I/O operation.
 * <p>
 * When the thread is interrupted by a call to {@code interrupt()}, it closes
 * the channel, sets the interrupt status of the thread to {@code true} and
 * throws a {@code ClosedByInterruptException}.
 */
public class ClosedByInterruptException extends AsynchronousCloseException {
    private static final long serialVersionUID = -4488191543534286750L;
    /**
     * Constructs a {@code ClosedByInterruptException}.
     */
    public ClosedByInterruptException() {
    }
}
