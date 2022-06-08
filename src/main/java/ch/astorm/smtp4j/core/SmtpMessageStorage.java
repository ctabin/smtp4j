
package ch.astorm.smtp4j.core;

import ch.astorm.smtp4j.SmtpServer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple {@code SmtpMessageHandler} that stores the received messages in a list.
 * This class is Thread-safe.
 */
public class SmtpMessageStorage implements SmtpMessageHandler,SmtpServerListener {
    private final List<SmtpMessage> messages = new ArrayList<>(32);
    private volatile boolean serverStarted;

    @Override
    public void notifyStart(SmtpServer server) {
        serverStarted = true;
    }

    @Override
    public void notifyClose(SmtpServer server) {
        synchronized(messages) {
            serverStarted = false;
            messages.notifyAll();
        }
    }

    @Override
    public void receive(SmtpMessage smtpMessage) {
        synchronized(messages) {
            messages.add(smtpMessage);
            messages.notifyAll();
        }
    }
    
    @Override
    public SmtpMessageReader messageReader() {
        return new SmtpMessageReader() {
            private List<SmtpMessage> localMessages = Collections.EMPTY_LIST;
            
            @Override
            public SmtpMessage readMessage() {
                if(!localMessages.isEmpty()) {
                    return localMessages.remove(0);
                }
                
                synchronized(messages) {
                    localMessages = readMessages();
                    while(serverStarted && localMessages.isEmpty()) {
                        try { messages.wait(); }
                        catch(InterruptedException ie) {}
                        
                        localMessages = readMessages();
                    }
                }
                
                return !localMessages.isEmpty() ? localMessages.remove(0) : null;
            }

            @Override
            public void close() throws Exception {
                /* nothing */
            }
        };
    }
    
    @Override
    public List<SmtpMessage> readMessages() {
        if(!serverStarted) { return Collections.EMPTY_LIST; }
        
        synchronized(messages) {
            List<SmtpMessage> copyMsgs = new ArrayList<>(messages);
            messages.clear();
            return copyMsgs;
        }
    }
}
