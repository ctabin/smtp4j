
package ch.astorm.smtp4j.auth;

import ch.astorm.smtp4j.SmtpServerOptions;
import ch.astorm.smtp4j.protocol.SmtpCommand;
import ch.astorm.smtp4j.protocol.SmtpProtocolConstants;
import ch.astorm.smtp4j.protocol.SmtpProtocolException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Random;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Handles the {@code CRAM-MD5} authentication scheme.
 * See <a href="https://www.samlogic.net/articles/smtp-commands-reference-auth.htm">here</a>.
 */
public class CramMD5AuthenticationHandler implements SmtpAuthenticatorHandler {
    private static final Random RANDOM = new SecureRandom();

    /**
     * Singleton instance of this handler.
     */
    public static CramMD5AuthenticationHandler INSTANCE = new CramMD5AuthenticationHandler();

    private CramMD5AuthenticationHandler() {}
    
    @Override
    public String getName() {
        return "CRAM-MD5";
    }

    @Override
    public boolean authenticate(SmtpCommand command, SmtpExchangeHandler exchangeHandler, SmtpServerOptions options) throws SmtpProtocolException {
        long rnd = (long)(RANDOM.nextDouble()*900000L)+100000L;
        long tms = System.currentTimeMillis();

        StringBuilder challengeBuilder = new StringBuilder(64);
        challengeBuilder.append("<");
        challengeBuilder.append(rnd).append("-").append(tms);
        challengeBuilder.append("@smtp4j.ch>");

        String challenge = challengeBuilder.toString();

        Encoder encoder = Base64.getEncoder();
        String encodedChallenge = encoder.encodeToString(challenge.getBytes(StandardCharsets.UTF_8));

        exchangeHandler.reply(SmtpProtocolConstants.CODE_SERVER_CHALLENGE, encodedChallenge);

        String encodedResponse = exchangeHandler.nextLine();

        Decoder decoder = Base64.getDecoder();
        String response = new String(decoder.decode(encodedResponse), StandardCharsets.UTF_8);

        int space = response.indexOf(' ');
        String username = response.substring(0, space);
        String hexChallenge = response.substring(space+1);
        
        try {
            options.usersRepository.getAuthenticator().checkChallenge(username, hexChallenge, key -> hashWithHMACMD5(challenge, key));
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    private static String hashWithHMACMD5(String string, byte[] key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "HmacMD5");
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(keySpec);

            byte[] bytes = mac.doFinal(string.getBytes(StandardCharsets.UTF_8));

            StringBuilder hash = new StringBuilder(32);
            for(byte aByte : bytes) {
                String hex = Integer.toHexString(0xFF & aByte);
                if(hex.length() == 1) { hash.append('0'); }
                hash.append(hex);
            }
            
            return hash.toString();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
