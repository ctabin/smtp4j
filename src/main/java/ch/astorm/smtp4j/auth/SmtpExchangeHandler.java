
package ch.astorm.smtp4j.auth;

import ch.astorm.smtp4j.protocol.SmtpProtocolException;

/**
 * Some {@link SmtpAuthenticatorHandler} requires multiple exchanges between the server
 * and the client. This interface provides simple methods to read-write between them.
 */
public interface SmtpExchangeHandler {
    /**
     * Reads the next line from the client.
     */
    String nextLine() throws SmtpProtocolException;

    /**
     * Replies the specified {@code code} and {@code message} to the client.
     *
     * @param code The SMTP code.
     * @param message The message.
     */
    void reply(int code, String message);

    /**
     * Replies the specified {@code code} to the client.
     *
     * @param code The SMTP code.
     */
    default void reply(int code) {
        reply(code, null);
    }
}
