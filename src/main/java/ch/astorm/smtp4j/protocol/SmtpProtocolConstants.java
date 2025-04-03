
package ch.astorm.smtp4j.protocol;

/**
 * Constants for the SMTP protocol.
 */
public class SmtpProtocolConstants {

    /**
     * Line/command break.
     * See the <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.1.1">specification</a>.
     */
    public static String CRLF = "\r\n";

    /**
     * Command-parameter separator.
     * See the <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.1.1">specification</a>.
     */
    public static String SP_FINAL = " ";
    
    /**
     * Command-parameter separator (multiline reply).
     * See the <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.1.3">specification</a>.
     */
    public static String SP_CONTINUE = "-";

    /**
     * Colon separator.
     * See the <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-3.3">specification</a>.
     */
    public static String COLON = ":";

    /**
     * Data dot-line separator.
     * See the <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-3.3">specification</a>.
     */
    public static String DOT = ".";

    /**
     * Code of the first reply of the SMTP server.
     */
    public static final int CODE_CONNECT = 220;

    /**
     * Code when the command is accepted by the server.
     */
    public static final int CODE_OK = 250;

    /**
     * Code when the command is not supported.
     */
    public static final int CODE_NOT_SUPPORTED = 255;

    /**
     * Code when the command is accepted, but in intermediate state (data).
     */
    public static final int CODE_INTERMEDIATE_REPLY = 354;

    /**
     * Error code when TLS is unavailable.
     */
    public static final int CODE_COMMAND_TLS_UNAVAILABLE = 454;
    
    /**
     * Error code when a command is unknown.
     */
    public static final int CODE_COMMAND_UNKNOWN = 500;

    /**
     * Error code when a bad sequence of command has been received.
     */
    public static final int CODE_BAD_COMMAND_SEQUENCE = 503;
    
    /**
     * Error code when a transaction has failed.
     */
    public static final int CODE_TRANSACTION_FAILED = 554;
}
