
package ch.astorm.smtp4j.store;

/**
 * Represents a user's repository.
 * Users are not linked to mailboxes. They are essentially determining who can send
 * messages to the SMTP server.
 */
public interface UserRepository {

    /**
     * Adds the given {@code username} to the repository.
     */
    void addUser(String username, String password);

    /**
     * Removes the given {@code username} from the repository.
     */
    void removeUser(String username);

    /**
     * Returns an {@link UserAuthenticator} for this repository.
     */
    UserAuthenticator getAuthenticator();
}
