
package ch.astorm.smtp4j.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple {@code SmtpMessageHandler} that stores the received messages in a list.
 * This class is Thread-safe.
 */
public class SmtpMessageStorage implements SmtpMessageHandler {
    private final List<SmtpMessage> messages = new ArrayList<>(32);

    @Override
    public void handle(SmtpMessage smtpMessage) {
        synchronized(messages) {
            messages.add(smtpMessage);
        }
    }

    /**
     * Returns a {@code List} with all the received messages.
     *
     * @return All the received messages.
     */
    public List<SmtpMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Clears all the messages.
     */
    public void clear() {
        synchronized(messages) {
            messages.clear();
        }
    }
}
