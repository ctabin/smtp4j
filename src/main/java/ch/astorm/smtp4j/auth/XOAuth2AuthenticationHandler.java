
package ch.astorm.smtp4j.auth;

import ch.astorm.smtp4j.SmtpServerOptions;
import ch.astorm.smtp4j.protocol.SmtpCommand;
import ch.astorm.smtp4j.protocol.SmtpProtocolException;
import ch.astorm.smtp4j.store.UserAuthenticator;
import jakarta.mail.PasswordAuthentication;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Handles {@code XOAUTH2} authentication scheme.
 * See <a href="https://learn.microsoft.com/en-us/exchange/client-developer/legacy-protocols/how-to-authenticate-an-imap-pop-smtp-application-by-using-oauth#sasl-xoauth2">here</a>.
 */
public class XOAuth2AuthenticationHandler implements SmtpAuthenticatorHandler {
    private static final byte CONTROL_A = '\u0001';
    
    /**
     * Singleton instance of this handler.
     */
    public static XOAuth2AuthenticationHandler INSTANCE = new XOAuth2AuthenticationHandler();
    
    @Override
    public String getName() {
        return "XOAUTH2";
    }

    @Override
    public boolean authenticate(SmtpCommand command, SmtpExchangeHandler exchangeHandler, SmtpServerOptions options) throws SmtpProtocolException {
        String parameter = command.getParameter();

        int space = parameter.indexOf(' ');
        if(space<0) { throw new SmtpProtocolException("Invalid credentials format"); }
        String base64Credentials = parameter.substring(space+1);

        byte[] decodedCredentials = Base64.getDecoder().decode(base64Credentials);
        String decodedStr = new String(decodedCredentials, StandardCharsets.UTF_8);
        int separator = decodedStr.indexOf(CONTROL_A);
        if(separator<0) { throw new SmtpProtocolException("Invalid credential format"); }
        
        String userProp = decodedStr.substring(0, separator);
        
        if(!userProp.toLowerCase().startsWith("user=")) { throw new SmtpProtocolException("Invalid user prop in credentials"); }
        
        String authProp = decodedStr.substring(separator+1);
        if(!authProp.toLowerCase().startsWith("auth=bearer ")) { throw new SmtpProtocolException("Invalid auth prop in credentials"); }
        
        String user = userProp.substring("user=".length()).trim();
        String token = authProp.substring("auth=bearer ".length()).trim();

        UserAuthenticator authenticator = options.usersRepository.getAuthenticator();
        try {
            authenticator.checkCredentials(new PasswordAuthentication(user, token));
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    @Override
    public void setSessionProperties(Properties properties, SmtpServerOptions options) {
        String protocolName = options.protocol.name().toLowerCase();
        properties.put("mail."+protocolName+".sasl.enable", "true");
        properties.put("mail."+protocolName+".sasl.mechanisms", "XOAUTH2");
    }
}
