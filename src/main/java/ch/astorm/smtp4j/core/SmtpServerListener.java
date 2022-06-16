
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

    /**
     * Invoked when the {@code server} has received a message.
     * This method will be invoked within the background {@code Thread} used to receive
     * the message.
     *
     * @param server The {@code SmtpServer} that received the message.
     * @param smtpMessage The received message.
     */
    void notifyMessage(SmtpServer server, SmtpMessage smtpMessage);
}
