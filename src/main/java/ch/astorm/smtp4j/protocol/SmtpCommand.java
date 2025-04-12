
package ch.astorm.smtp4j.protocol;

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
    public static enum Type {
        EHLO("EHLO", "HELO"),
        QUIT("QUIT"),
        AUTH("AUTH"),
        MAIL_FROM("MAIL FROM:"),
        RECIPIENT("RCPT TO:"),
        DATA("DATA"),
        EXPAND("EXPN"),
        VERIFY("VRFY"),
        NOOP("NOOP"),
        STARTTLS("STARTTLS"),
        HELP("HELP"),
        RESET("RSET"),
        UNKNOWN("#UNKN#");

        private final List<String> cmds;
        private Type(String... cmds) { this.cmds = Arrays.asList(cmds); }
        public boolean matches(String cmd) { return this.cmds.contains(cmd.toUpperCase(Locale.ROOT)); }
    }

    /**
     * Creates a new {@code SmtpCommand}.
     *
     * @param type The command type.
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
        return type+(parameter!=null ? " "+parameter : "");
    }

    /**
     * Parses the {@code line} and creates a new {@code SmtpCommand}.
     * If {@code line} is null, this method directly returns null. In all other cases,
     * a new {@code SmtpCommand} will be returned.
     *
     * @param line The SMTP line.
     * @return A new {@code SmtpCommand} or null.
     */
    public static SmtpCommand parse(String line) {
        if(line==null) { return null; }

        String command;
        String parameter;
        int colon = line.indexOf(SmtpProtocolConstants.COLON);
        if(colon>=0) {
            command = line.substring(0, colon+1);
            parameter = line.substring(colon+1).trim();
        } else {
            int firstSpace = line.indexOf(SmtpProtocolConstants.SP_FINAL);
            command = firstSpace<0 ? line : line.substring(0, firstSpace);
            parameter = firstSpace<0 ? null : line.substring(firstSpace+1).trim();
        }

        for(Type type : Type.values()) {
            if(type.matches(command)) {
                return new SmtpCommand(type, parameter);
            }
        }

        return new SmtpCommand(Type.UNKNOWN, line);
    }
}
