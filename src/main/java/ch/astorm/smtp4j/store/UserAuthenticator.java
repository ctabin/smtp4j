
package ch.astorm.smtp4j.store;

import jakarta.mail.PasswordAuthentication;

/**
 * Handles the authentication of users.
 */
public interface UserAuthenticator {

    /**
     * Checks the given {@code credentials} and throws a {@code SecurityException}
     * if those are invalid.
     */
    void checkCredentials(PasswordAuthentication credentials) throws SecurityException;
}
