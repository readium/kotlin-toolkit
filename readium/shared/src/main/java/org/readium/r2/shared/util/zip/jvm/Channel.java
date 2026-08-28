package org.readium.r2.shared.util.zip.jvm;

import java.io.Closeable;
import java.io.IOException;

/**
 * A channel is a conduit to I/O services covering such items as files, sockets,
 * hardware devices, I/O ports or some software component.
 * <p>
 * Channels are open upon creation, and can be closed explicitly. Once a channel
 * is closed it cannot be re-opened, and any attempts to perform I/O operations
 * on the closed channel result in a <code>ClosedChannelException</code>.
 * <p>
 * Particular implementations or sub-interfaces of {@code Channel} dictate
 * whether they are thread-safe or not.
 */
public interface Channel extends Closeable {
    /**
     * Returns true if this channel is open.
     */
    public boolean isOpen();
    /**
     * Closes an open channel. If the channel is already closed then this method
     * has no effect. If there is a problem with closing the channel then the
     * method throws an IOException and the exception contains reasons for the
     * failure.
     * <p>
     * If an attempt is made to perform an operation on a closed channel then a
     * {@link ClosedChannelException} will be thrown on that attempt.
     * <p>
     * If multiple threads attempt to simultaneously close a channel, then only
     * one thread will run the closure code, and others will be blocked until
     * the first returns.
     *
     * @throws IOException
     *             if a problem occurs closing the channel.
     */
    public void close() throws IOException;
}
