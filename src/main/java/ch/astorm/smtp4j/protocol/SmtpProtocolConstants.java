/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

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
    public static String SP = " ";

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
     * Code when the command is forbidden, e.g., by the {@link ch.astorm.smtp4j.firewall.SmtpFirewall}.
     */
    public static final int CODE_FORBIDDEN = 403;

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
