
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
    public static char DOT = '.';

    /**
     * Code of the first reply of the SMTP server.
     */
    public static final int CODE_CONNECT = 220;
    
    /**
     * Code replied when the {@code QUIT} command is received.
     */
    public static final int CODE_QUIT = 221;

    /**
     * Code replied when authentication has been successful.
     */
    public static final int CODE_AUTHENTICATION_SUCCESS = 235;

    /**
     * Code when the command is accepted by the server.
     */
    public static final int CODE_OK = 250;

    /**
     * Code when the command is not supported.
     */
    public static final int CODE_NOT_SUPPORTED = 255;

    /**
     * Code when an intermediate challenge is asked by the server during authentication.
     */
    public static final int CODE_SERVER_CHALLENGE = 334;

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
     * Error code when an invalid authentication scheme is used.
     */
    public static final int CODE_BAD_AUTHENTICATION_SCHEME = 504;
    
    /**
     * Error code when authentication is required.
     */
    public static final int CODE_AUTHENTICATION_REQUIRED = 530;

    /**
     * Error code when authentication failed.
     */
    public static final int CODE_AUTHENTICATION_FAILURE = 535;

    /**
     * Error code when a transaction has failed.
     */
    public static final int CODE_TRANSACTION_FAILED = 554;
}
