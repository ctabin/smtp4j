
package ch.astorm.smtp4j.auth;

import ch.astorm.smtp4j.SmtpServerOptions;
import ch.astorm.smtp4j.protocol.SmtpCommand;
import ch.astorm.smtp4j.protocol.SmtpProtocolConstants;
import ch.astorm.smtp4j.protocol.SmtpProtocolException;
import ch.astorm.smtp4j.store.UserAuthenticator;
import jakarta.mail.PasswordAuthentication;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * Handles {@code PLAIN} authentication scheme.
 * See <a href="https://www.samlogic.net/articles/smtp-commands-reference-auth.htm">here</a>.
 */
public class PlainAuthenticationHandler implements SmtpAuthenticatorHandler {

    /**
     * Singleton instance of this handler.
     */
    public static PlainAuthenticationHandler INSTANCE = new PlainAuthenticationHandler();

    private PlainAuthenticationHandler() {}

    @Override
    public String getName() {
        return "PLAIN";
    }

    @Override
    public boolean authenticate(SmtpCommand command, SmtpExchangeHandler exchangeHandler, SmtpServerOptions options) throws SmtpProtocolException {
        String parameter = command.getParameter();

        String base64Credentials;
        int space = parameter.indexOf(' ');
        if(space<0) {
            exchangeHandler.reply(SmtpProtocolConstants.CODE_SERVER_CHALLENGE);
            base64Credentials = exchangeHandler.nextLine();
        } else {
            base64Credentials = parameter.substring(space+1);
        }

        byte[] decodedCredentials = Base64.getDecoder().decode(base64Credentials);
        int firstNul = -1;
        int secondNul = -1;
        for(int i=0 ; i<decodedCredentials.length ; ++i) {
            byte b = decodedCredentials[i];
            if(b==0) {
                if(firstNul<0) { firstNul = i; }
                else if(secondNul<0) {
                    secondNul = i;
                    break;
                }
            }
        }

        if(firstNul<0 || secondNul<0) {
            throw new SmtpProtocolException("Invalid credentials format");
        }

        //https://www.ietf.org/rfc/rfc4616.txt
        /*
        The mechanism consists of a single message, a string of [UTF-8]
        encoded [Unicode] characters, from the client to the server.  The
        client presents the authorization identity (identity to act as),
        followed by a NUL (U+0000) character, followed by the authentication
        identity (identity whose password will be used), followed by a NUL
        (U+0000) character, followed by the clear-text password.  As with
        other SASL mechanisms, the client does not provide an authorization
        identity when it wishes the server to derive an identity from the
        credentials and use that as the authorization identity.
        */
        String authorizationIdentity = new String(Arrays.copyOfRange(decodedCredentials, 0, firstNul), StandardCharsets.UTF_8);
        String authenticationIdentity = new String(Arrays.copyOfRange(decodedCredentials, firstNul+1, secondNul), StandardCharsets.UTF_8);
        String clearTextPassword = new String(Arrays.copyOfRange(decodedCredentials, secondNul+1, decodedCredentials.length), StandardCharsets.UTF_8);

        if(options.usersRepository==null) {
            return false;
        }

        UserAuthenticator authenticator = options.usersRepository.getAuthenticator();
        try {
            authenticator.checkCredentials(new PasswordAuthentication(authenticationIdentity, clearTextPassword));
            return true;
        } catch(Exception e) {
            return false;
        }
    }
}
