
package ch.astorm.smtp4j.protocol;

import ch.astorm.smtp4j.SmtpServer;
import ch.astorm.smtp4j.protocol.DefaultSmtpTransactionHandler.MessageReceiver;

/**
 * Creates instances of {@link SmtpTransactionHandler}.
 */
public interface SmtpTransactionHandlerFactory {
    /**
     * Creates a new {@link SmtpTransactionHandler} instance.
     *
     * @param server The {@link SmtpServer} that received the connection.
     * @param receiver The {@link MessageReceiver} to receive the SMTP messages.
     * @return A new instance.
     */
    SmtpTransactionHandler create(SmtpServer server, MessageReceiver receiver);
}
