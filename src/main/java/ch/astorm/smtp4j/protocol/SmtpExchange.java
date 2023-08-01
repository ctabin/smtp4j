
package ch.astorm.smtp4j.protocol;

import java.util.List;

/**
 * Protocol exchange data between smtp4j and the sender.
 */
public class SmtpExchange {
    private final List<String> received;
    private final String replied;
    
    public SmtpExchange(List<String> received, String replied) {
        this.received = received;
        this.replied = replied;
    }
    
    /**
     * Returns the data received by smtp4j.
     * Most of the messages will only contain one item. In the case of the {@link SmtpCommand.Type#DATA}
     * command, there might be multiple items (each item corresponds to one line in the SMTP protocol).
     *
     * @return The received data.
     */
    public List<String> getReceivedData() {
        return received;
    }
    
    /**
     * Returns the response sent back by smtp4j:
     *
     * @return The response data.
     */
    public String getRepliedData() {
        return replied;
    }
}
