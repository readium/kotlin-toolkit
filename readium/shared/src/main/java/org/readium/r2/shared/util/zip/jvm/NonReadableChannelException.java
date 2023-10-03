package org.readium.r2.shared.util.zip.jvm;

/**
 * A {@code NonReadableChannelException} is thrown when attempting to read from
 * a channel that is not open for reading.
 */
public class NonReadableChannelException extends IllegalStateException {
    private static final long serialVersionUID = -3200915679294993514L;
    /**
     * Constructs a {@code NonReadableChannelException}.
     */
    public NonReadableChannelException() {
    }
}
