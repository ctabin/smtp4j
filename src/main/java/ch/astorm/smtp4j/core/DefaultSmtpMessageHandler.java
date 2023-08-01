
package ch.astorm.smtp4j.core;

import ch.astorm.smtp4j.SmtpServer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Simple {@code SmtpMessageHandler} that stores the received messages in a list.
 * This class is Thread-safe.
 */
public class DefaultSmtpMessageHandler implements SmtpMessageHandler {
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
    public void notifyMessage(SmtpServer server, SmtpMessage smtpMessage) {
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
                    localMessages = readMessages(1, TimeUnit.SECONDS);
                    while(serverStarted && localMessages.isEmpty()) {
                        localMessages = readMessages(1, TimeUnit.SECONDS);
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
    public List<SmtpMessage> readMessages(long delayIfNoMessage, TimeUnit unit) {
        if(!serverStarted) { return Collections.EMPTY_LIST; }
        
        synchronized(messages) {
            if(messages.isEmpty() && delayIfNoMessage>=0) {
                try { messages.wait(TimeUnit.MILLISECONDS.convert(delayIfNoMessage, unit)); }
                catch(InterruptedException ie) { }
            }
            
            if(messages.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            
            List<SmtpMessage> copyMsgs = new ArrayList<>(messages);
            messages.clear();
            return copyMsgs;
        }
    }
}
