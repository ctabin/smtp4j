
package ch.astorm.smtp4j.protocol;

import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.protocol.SmtpCommand.Type;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the SMTP protocol.
 */
public class SmtpTransactionHandler {
    private final BufferedReader input;
    private final PrintWriter output;
    private final MessageReceiver messageReceiver;

    /**
     * Represents a message receiver within the SMTP transaction.
     */
    @FunctionalInterface
    public static interface MessageReceiver {

        /**
         * Invoked when a message is received.
         *
         * @param message The received message.
         */
        void receiveMessage(SmtpMessage message);
    }

    private SmtpTransactionHandler(BufferedReader input, PrintWriter output, MessageReceiver messageReceiver) {
        this.input = input;
        this.output = output;
        this.messageReceiver = messageReceiver;
    }

    /**
     * Handles the SMTP protocol communication.
     *
     * @param input The input scanner.
     * @param output The output writer.
     * @param messageReceiver The {@code MessageReceiver}.
     */
    public static void handle(BufferedReader input, PrintWriter output, MessageReceiver messageReceiver) throws IOException, SmtpProtocolException {
        SmtpTransactionHandler sth = new SmtpTransactionHandler(input, output, messageReceiver);
        sth.execute();
    }

    private void execute() throws SmtpProtocolException {
        //inform the client about the SMTP server state
        reply(SmtpProtocolConstants.CODE_CONNECT, "localhost smtp4j server ready");

        //extends the EHLO/HELO command to greet the client
        SmtpCommand ehlo = SmtpCommand.parse(nextLine());
        if(ehlo!=null) {
            if(ehlo.getType()==Type.EHLO) {
                String param = ehlo.getParameter();
                reply(SmtpProtocolConstants.CODE_OK, param!=null ? "smtp4j greets "+ehlo.getParameter() : "OK");
            } else {
                reply(SmtpProtocolConstants.CODE_BAD_COMMAND_SEQUENCE, "Bad sequence of command (wrong command)");
                return;
            }
        } else {
            reply(SmtpProtocolConstants.CODE_BAD_COMMAND_SEQUENCE, "Bad sequence of command (no more token)");
            return;
        }

        //start reading the transaction data
        readTransaction();
    }

    private String mailFrom;
    private List<String> recipients;
    private StringBuilder smtpMessageContent;

    private void readTransaction() throws SmtpProtocolException {
        while(true) {
            SmtpCommand command = nextCommand();
            Type commandType = command.getType();

            if(mailFrom==null) {
                if(commandType==Type.MAIL_FROM) {
                    String enbraced = command.getParameter(); //enclosed: <mail_value>
                    mailFrom = enbraced.substring(1, enbraced.length()-1);
                    reply(SmtpProtocolConstants.CODE_OK, "OK");
                } else if(commandType==Type.QUIT) {
                    reply(SmtpProtocolConstants.CODE_OK, "OK");
                    return;
                } else {
                    reply(SmtpProtocolConstants.CODE_BAD_COMMAND_SEQUENCE, "Bad sequence of command (wrong command)");
                }
                continue;
            } 

            if(recipients==null) {
                if(commandType!=Type.RECIPIENT) {
                    reply(SmtpProtocolConstants.CODE_BAD_COMMAND_SEQUENCE, "Bad sequence of command (wrong command)");
                    continue;
                }

                recipients = new ArrayList<>();
                while(commandType==Type.RECIPIENT) {
                    String enbraced = command.getParameter(); //enclosed: <mail_value>
                    recipients.add(enbraced.substring(1, enbraced.length()-1));
                    reply(SmtpProtocolConstants.CODE_OK, "OK");
                    
                    command = nextCommand();
                    commandType = command.getType();
                }
            }

            if(commandType==Type.DATA) {
                if(smtpMessageContent!=null) {
                    reply(SmtpProtocolConstants.CODE_BAD_COMMAND_SEQUENCE, "Bad sequence of command (wrong command)");
                    continue;
                }

                smtpMessageContent = new StringBuilder(256);
                reply(SmtpProtocolConstants.CODE_INTERMEDIATE_REPLY, null);

                String currentLine = nextLine();
                while(currentLine!=null) {
                    //DATA content must end with a dot on a single line
                    if(currentLine.equals(SmtpProtocolConstants.DOT)) {
                        smtpMessageContent.delete(smtpMessageContent.length()-SmtpProtocolConstants.CRLF.length(), smtpMessageContent.length());
                        break;
                    } else {
                        //if DATA starts with a dot, a second one must be added
                        if(currentLine.startsWith(SmtpProtocolConstants.DOT)) { currentLine = currentLine.substring(1); }
                        smtpMessageContent.append(currentLine).append(SmtpProtocolConstants.CRLF);
                    }

                    currentLine = nextLine();
                }

                SmtpMessage message = SmtpMessage.create(mailFrom, recipients, smtpMessageContent.toString());
                messageReceiver.receiveMessage(message);

                reply(SmtpProtocolConstants.CODE_OK, "OK");
                continue;
            }

            if(commandType==Type.QUIT) {
                reply(SmtpProtocolConstants.CODE_OK, "OK");
                return;
            } else {
                reply(SmtpProtocolConstants.CODE_BAD_COMMAND_SEQUENCE, "Bad sequence of command (wrong command)");
            }
        }
    }

    private void resetState() {
        this.mailFrom = null;
        this.recipients = null;
        this.smtpMessageContent = null;
    }

    private String nextLine() throws SmtpProtocolException {
        try {
            String line = input.readLine();
            if(line==null) { throw new SmtpProtocolException("Unexpected end of stream (no more line)"); }
            return line;
        } catch(IOException ioe) {
            throw new SmtpProtocolException("I/O exception", ioe);
        }
    }
    
    private SmtpCommand nextCommand() throws SmtpProtocolException {
        SmtpCommand command = SmtpCommand.parse(nextLine());
        while(command!=null) {
            Type commandType = command.getType();
            if(commandType==Type.NOOP) { reply(SmtpProtocolConstants.CODE_OK, "OK"); }
            else if(commandType==Type.EXPAND) { reply(SmtpProtocolConstants.CODE_NOT_SUPPORTED, "Not supported"); }
            else if(commandType==Type.VERIFY) { reply(SmtpProtocolConstants.CODE_NOT_SUPPORTED, "Not supported"); }
            else if(commandType==Type.HELP) { reply(SmtpProtocolConstants.CODE_NOT_SUPPORTED, "Not supported"); }
            else if(commandType==Type.UNKNOWN) { reply(SmtpProtocolConstants.CODE_COMMAND_UNKNOWN, "Unknown command"); }
            else if(commandType==Type.RESET) { resetState(); reply(SmtpProtocolConstants.CODE_OK, "OK"); }
            else { return command; }

            command = SmtpCommand.parse(nextLine());
        }

        throw new SmtpProtocolException("Unexpected end of exchange (no more command)");
    }

    private void reply(int code, String message) {
        StringBuilder builder = new StringBuilder(32);
        builder.append(code);
        if(message!=null) {
            builder.append(SmtpProtocolConstants.SP);
            builder.append(message);
        }
        builder.append(SmtpProtocolConstants.CRLF);

        output.print(builder.toString());
        output.flush();
    }
}
