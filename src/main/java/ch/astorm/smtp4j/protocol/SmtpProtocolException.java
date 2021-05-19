
package ch.astorm.smtp4j.protocol;

/**
 * Exception thrown when there is an error during the protocol exchange.
 */
public class SmtpProtocolException extends Exception {
    public SmtpProtocolException(String message) { super(message); }
    public SmtpProtocolException(String message, Throwable cause) { super(message, cause); }
}
