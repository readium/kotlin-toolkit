package org.readium.r2.shared.util.zip.jvm;

/**
 * A {@code NonWritableChannelException} is thrown when attempting to write to a
 * channel that is not open for writing.
 */
public class NonWritableChannelException extends IllegalStateException {
    private static final long serialVersionUID = -7071230488279011621L;
    /**
     * Constructs a {@code NonWritableChannelException}.
     */
    public NonWritableChannelException() {
    }
}
