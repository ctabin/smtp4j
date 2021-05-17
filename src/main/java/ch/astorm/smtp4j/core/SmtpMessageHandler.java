
package ch.astorm.smtp4j.core;

/**
 * Represents a message handler that will process any incoming message.
 */
public interface SmtpMessageHandler {

    /**
     * Handles the specified transaction.
     *
     * @param smtpMessage The message received within the transaction.
     */
    void handle(SmtpMessage smtpMessage);
}
