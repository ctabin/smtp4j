
package ch.astorm.smtp4j.store;

import ch.astorm.smtp4j.auth.CramMD5AuthenticationHandler;
import jakarta.mail.PasswordAuthentication;
import java.util.function.Function;

/**
 * Handles the authentication of users.
 */
public interface UserAuthenticator {

    /**
     * Checks the given {@code credentials} and throws a {@code SecurityException}
     * if those are invalid.
     */
    void checkCredentials(PasswordAuthentication credentials) throws SecurityException;

    /**
     * Checks if the given {@code responseChallenge} matches the expected value returned by the {@code hashFunc}
     * according to the {@code username}'s password.
     * @see CramMD5AuthenticationHandler
     */
    void checkChallenge(String username, String responseChallenge, Function<byte[], String> hashFunc) throws SecurityException;
}
