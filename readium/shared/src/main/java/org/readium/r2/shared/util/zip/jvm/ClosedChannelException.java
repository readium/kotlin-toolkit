package org.readium.r2.shared.util.zip.jvm;

import java.io.IOException;
/**
 * A {@code ClosedChannelException} is thrown when a channel is closed for the
 * type of operation attempted.
 */
public class ClosedChannelException extends IOException {
    private static final long serialVersionUID = 882777185433553857L;
    /**
     * Constructs a {@code ClosedChannelException}.
     */
    public ClosedChannelException() {
    }
}
