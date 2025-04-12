
package ch.astorm.smtp4j.auth;

import ch.astorm.smtp4j.SmtpServerOptions;
import ch.astorm.smtp4j.protocol.SmtpCommand;
import ch.astorm.smtp4j.protocol.SmtpProtocolException;

/**
 * Represents an authenticator handler.
 */
public interface SmtpAuthenticatorHandler {
    /**
     * Returns the name in the SMTP protocol.
     * For instance `PLAIN`.
     */
    String getName();

    /**
     * Authenticates the client.
     *
     * @param command The initial {@code AUTH} command with its parameters.
     * @param exchangeHandler The exchange handler if challenge responses from the client is needed.
     * @param options The options.
     * @return true if the user has been authenticated, false otherwise (invalid credentials).
     */
    boolean authenticate(SmtpCommand command, SmtpExchangeHandler exchangeHandler, SmtpServerOptions options) throws SmtpProtocolException;
}
