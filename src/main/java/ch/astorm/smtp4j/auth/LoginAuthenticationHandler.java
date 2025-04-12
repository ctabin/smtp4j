
package ch.astorm.smtp4j.auth;

import ch.astorm.smtp4j.SmtpServerOptions;
import ch.astorm.smtp4j.protocol.SmtpCommand;
import ch.astorm.smtp4j.protocol.SmtpProtocolConstants;
import ch.astorm.smtp4j.protocol.SmtpProtocolException;
import ch.astorm.smtp4j.store.UserAuthenticator;
import jakarta.mail.PasswordAuthentication;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

/**
 * Handles the {@code LOGIN} authentication scheme.
 * See <a href="https://www.samlogic.net/articles/smtp-commands-reference-auth.htm">here</a>.
 */
public class LoginAuthenticationHandler implements SmtpAuthenticatorHandler {

    /**
     * Singleton instance of this handler.
     */
    public static LoginAuthenticationHandler INSTANCE = new LoginAuthenticationHandler();

    private LoginAuthenticationHandler() {}

    @Override
    public String getName() {
        return "LOGIN";
    }

    @Override
    public boolean authenticate(SmtpCommand command, SmtpExchangeHandler exchangeHandler, SmtpServerOptions options) throws SmtpProtocolException {
        Encoder encoder = Base64.getEncoder();
        Decoder decoder = Base64.getDecoder();

        String respUsernameEncoded = encoder.encodeToString("Username".getBytes(StandardCharsets.UTF_8));
        exchangeHandler.reply(SmtpProtocolConstants.CODE_SERVER_CHALLENGE, respUsernameEncoded);
        String username = new String(decoder.decode(exchangeHandler.nextLine()), StandardCharsets.UTF_8);

        String respPasswordEncoded = encoder.encodeToString("Password".getBytes(StandardCharsets.UTF_8));
        exchangeHandler.reply(SmtpProtocolConstants.CODE_SERVER_CHALLENGE, respPasswordEncoded);
        String password = new String(decoder.decode(exchangeHandler.nextLine()), StandardCharsets.UTF_8);

        UserAuthenticator authenticator = options.usersRepository.getAuthenticator();
        try {
            authenticator.checkCredentials(new PasswordAuthentication(username, password));
            return true;
        } catch(Exception e) {
            return false;
        }
    }
}
