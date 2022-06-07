
package ch.astorm.smtp4j.core;

import ch.astorm.smtp4j.SmtpServer;

/**
 * Represents a server listener.
 */
public interface SmtpServerListener {

    /**
     * Invoked when the {@code server} has been started and is ready to receive messages.
     *
     * @param server The started {@code SmtpServer}.
     */
    default void notifyStart(SmtpServer server) {}

    /**
     * Invoked when the {@code server} has been closed.
     *
     * @param server The stopped {@code SmtpServer}.
     */
    default void notifyClose(SmtpServer server) {}
}
