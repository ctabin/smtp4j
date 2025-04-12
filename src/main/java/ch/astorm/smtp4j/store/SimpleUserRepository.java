
package ch.astorm.smtp4j.store;

import jakarta.mail.PasswordAuthentication;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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

        @Override
        public void checkChallenge(String username, String responseChallenge, Function<byte[], String> hashFunc) throws SecurityException {
            String password = users.get(username);
            if(password==null) { throw new SecurityException("Invalid credentials"); }

            String expected = hashFunc.apply(password.getBytes(StandardCharsets.UTF_8));
            if(!expected.equals(responseChallenge)) { throw new SecurityException("Invalid credentials"); }
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
