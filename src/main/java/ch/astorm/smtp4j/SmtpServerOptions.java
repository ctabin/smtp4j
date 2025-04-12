
package ch.astorm.smtp4j;

import ch.astorm.smtp4j.auth.SmtpAuthenticatorHandler;
import ch.astorm.smtp4j.protocol.SmtpCommand;
import ch.astorm.smtp4j.secure.DefaultSSLContextProvider;
import ch.astorm.smtp4j.secure.SSLContextProvider;
import ch.astorm.smtp4j.store.SimpleUserRepository;
import ch.astorm.smtp4j.store.UserRepository;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Represents options of the {@link SmtpServer}.
 * Most of theses options can be set directly through the {@link SmtpServerBuilder}.
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
    public PrintStream debugStream;
    
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

    /**
     * String reply when a client connects to smtp4j.
     */
    public String connectionString = "localhost smtp4j server ready";

    /**
     * Function that generates the reply to the {@link SmtpCommand.Type#EHLO} command.
     * The function input is the parameter sent by the client in the protocol and might be null.
     */
    public Function<String, String> ehloResponseFunction = h -> h!=null ? "smtp4j greets "+h : "OK";

    /**
     * List of {@link SmtpAuthenticatorHandler}.
     * If this list has one item or more, an authentication will be required from the client.
     */
    public List<SmtpAuthenticatorHandler> authenticators = new ArrayList<>();

    /**
     * The users repository used for the authentication.
     */
    public UserRepository usersRepository = new SimpleUserRepository();
}
