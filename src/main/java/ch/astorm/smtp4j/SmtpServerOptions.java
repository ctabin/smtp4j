
package ch.astorm.smtp4j;

import ch.astorm.smtp4j.protocol.SmtpCommand;
import ch.astorm.smtp4j.secure.DefaultSSLContextProvider;
import ch.astorm.smtp4j.secure.SSLContextProvider;
import java.io.PrintStream;

/**
 * Represents options of the SMTP server.
 */
public class SmtpServerOptions {
    
    /**
     * Represents the protocol.
     */
    public static enum Protocol {
        /**
         * Simple mail transfer protocol.
         */
        SMTP,

        /**
         * Simple mail transfer protocol over TLS.
         */
        SMTPS
    }

    /**
     * Output for internal debugging. This stream will receive all the inputs/outputs
     * of the underlying SMTP protocol.
     * If {@code null}, then no debug will be printed.
     */
    public PrintStream debug;
    
    /**
     * True if the {@link SmtpCommand.Type#STARTTLS} command must be accepted.
     * In the client sends it, it will be still unsupported.
     */
    public boolean starttls = false;
    
    /**
     * The protocol to use.
     */
    public Protocol protocol = Protocol.SMTP;

    /**
     * The {@code SSLContextProvider} that will provide the {@code SSLContext} to
     * upgrade to TLS communication.
     * @see DefaultSSLContextProvider
     */
    public SSLContextProvider sslContextProvider;
}
