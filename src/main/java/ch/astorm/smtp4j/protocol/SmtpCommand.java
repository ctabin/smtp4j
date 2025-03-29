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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * List of SMTP commands.
 */
public class SmtpCommand {
    private final Type type;
    private final String parameter;

    /**
     * Represents the SMTP command types.
     */
    public enum Type {
        EHLO("EHLO", "HELO"),
        QUIT("QUIT"),
        MAIL_FROM("MAIL FROM:"),
        RECIPIENT("RCPT TO:"),
        DATA("DATA"),
        EXPAND("EXPN"),
        VERIFY("VRFY"),
        NOOP("NOOP"),
        HELP("HELP"),
        RESET("RSET"),
        SIZE("SIZE"),
        EIGHT_BIT_MIME("8BITMIME"),
        UNKNOWN("#UNKN#");

        private final List<String> cmds;

        Type(String... cmds) {
            this.cmds = Arrays.asList(cmds);
        }

        public boolean matches(String cmd) {
            return this.cmds.contains(cmd.toUpperCase(Locale.ROOT));
        }

        public String withArgs(String arg) {
            return cmds.getFirst() + (arg != null ? " " + arg : "");
        }
    }

    /**
     * Creates a new {@code SmtpCommand}.
     *
     * @param type  The command type.
     * @param param The parameter.
     */
    public SmtpCommand(Type type, String param) {
        this.type = type;
        this.parameter = param;
    }

    /**
     * Returns the command's type.
     *
     * @return The command's type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the command's parameter.
     *
     * @return The command's parameter.
     */
    public String getParameter() {
        return parameter;
    }

    /**
     * Returns this SMTP command in a printable format.
     *
     * @return The string representation of this command.
     */
    @Override
    public String toString() {
        return type + (parameter != null ? " " + parameter : "");
    }

    /**
     * Parses the {@code line} and creates a new {@code SmtpCommand}.
     * If {@code line} is null, this method directly returns null. In all other cases,
     * a new {@code SmtpCommand} will be returned.
     *
     * @param lineBytes The SMTP line.
     * @return A new {@code SmtpCommand} or null.
     */
    public static SmtpCommand parse(byte[] lineBytes) {
        if (lineBytes == null) {
            return null;
        }

        String line = new String(lineBytes, StandardCharsets.ISO_8859_1);

        String command;
        String parameter;
        int colon = line.indexOf(SmtpProtocolConstants.COLON);
        if (colon >= 0) {
            command = line.substring(0, colon + 1);
            parameter = line.substring(colon + 1).trim();
        } else {
            int firstSpace = line.indexOf(SmtpProtocolConstants.SP);
            command = firstSpace < 0 ? line : line.substring(0, firstSpace);
            parameter = firstSpace < 0 ? null : line.substring(firstSpace + 1).trim();
        }

        for (Type type : Type.values()) {
            if (type.matches(command)) {
                return new SmtpCommand(type, parameter);
            }
        }

        return new SmtpCommand(Type.UNKNOWN, line);
    }
}
