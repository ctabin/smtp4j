
package ch.astorm.smtp4j.store;

import jakarta.mail.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;

/**
 * A basic user repository.
 */
public class SimpleUserRepository implements UserRepository {
    private final Map<String, String> users = new HashMap<>();
    private final UserAuthenticator authenticator = new UserAuthenticator() {
        @Override
        public void checkCredentials(PasswordAuthentication credentials) throws SecurityException {
            String password = users.get(credentials.getUserName());
            if(password==null || !password.equals(credentials.getPassword())) { throw new SecurityException("Invalid credentials"); }
        }
    };

    /**
     * Adds the given {@code user} with its {@code password}.
     */
    @Override
    public void addUser(String user, String password) {
        users.put(user, password);
    }

    /**
     * Removes the given {@code user}.
     */
    @Override
    public void removeUser(String user) {
        users.remove(user);
    }

    @Override
    public UserAuthenticator getAuthenticator() {
        return authenticator;
    }
}
