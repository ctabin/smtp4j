
package ch.astorm.smtp4j.core;

import java.util.List;

/**
 * Represents a message handler that will process any incoming message.
 */
public interface SmtpMessageHandler extends SmtpServerListener {

    /**
     * Represents a simple {@code SmtpMessage} iterator.
     */
    static interface SmtpMessageReader extends AutoCloseable {
        
        /**
         * Reads the next available {@code SmtpMessage}.
         * If none, this method will block until a new one is received.
         * If the {@code SmtpServer} is closed, this method will return null.
         *
         * @return The next received {@code SmtpMessage} or null if the underlying {@code SmtpServer} is closed.
         */
        SmtpMessage readMessage();
    }
    
    /**
     * Returns a new {@code SmtpMessageReader} that loops over the received messages.
     * Note that if you create multiple {@code SmtpMessageReader} instances, the will
     * compete over the same message list and the messages will be received only by one
     * of the readers.
     *
     * @return A new {@code SmtpMessageReader} instance.
     */
    SmtpMessageReader messageReader();
    
    /**
     * Retrieves the received messages and clears the list.
     * If no new message has been received since the last notification, an empty
     * list will be returned.
     * 
     * @return All the (newly) received messages or an empty list if none.
     */
    List<SmtpMessage> readMessages();
}
