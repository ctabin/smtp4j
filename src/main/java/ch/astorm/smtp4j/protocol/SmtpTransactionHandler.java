
package ch.astorm.smtp4j.protocol;

import java.io.IOException;
import java.net.Socket;

/**
 * Handler of SMTP transactions.
 *
 * @see SmtpTransactionHandlerFactory
 */
public interface SmtpTransactionHandler extends AutoCloseable {
    
    /**
     * Proceeds the SMTP transaction with the given {@code socket}.
     * Once this method returns, both the {@code socket} and the handler will be closed.
     *
     * @param socket The socket.
     */
    void execute(Socket socket) throws IOException, SmtpProtocolException;
}
