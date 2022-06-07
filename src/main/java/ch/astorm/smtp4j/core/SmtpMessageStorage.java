
package ch.astorm.smtp4j.core;

import java.util.ArrayList;
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
     * Retrieves the received messages and clears the list.
     * If no new message has been received since the last notification, an empty
     * list will be returned.
     * 
     * @return All the (newly) received messages or an empty list if none.
     */
    public List<SmtpMessage> readMessages() {
        synchronized (messages) {
            List<SmtpMessage> copyMsgs = new ArrayList<>(messages);
            messages.clear();
            return copyMsgs;
        }
    }
    
    /**
     * Returns a {@code List} with all the received messages.
     *
     * @return All the received messages.
     * @deprecated Use {@link #readMessages()} instead.
     */
    @Deprecated
    public List<SmtpMessage> getMessages() {
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }

    /**
     * Clears all the messages.
     * 
     * @deprecated Use {@link #readMessages()} instead.
     */
    @Deprecated
    public void clear() {
        synchronized(messages) {
            messages.clear();
        }
    }
}
