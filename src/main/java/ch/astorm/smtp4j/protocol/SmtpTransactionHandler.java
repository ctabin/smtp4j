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

import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.firewall.SmtpFirewall;
import ch.astorm.smtp4j.protocol.SmtpCommand.Type;
import ch.astorm.smtp4j.util.ByteArrayUtils;
import ch.astorm.smtp4j.util.LineAwareBufferedInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the SMTP protocol.
 */
public class SmtpTransactionHandler {
    private final LineAwareBufferedInputStream input;
    private final PrintWriter output;
    private final MessageReceiver messageReceiver;
    private final SmtpFirewall firewall;
    private final Long maxMessageSize;

    /**
     * Represents a message receiver within the SMTP transaction.
     */
    @FunctionalInterface
    public interface MessageReceiver {

        /**
         * Invoked when a message is received.
         * If this method throws an exception, the error will be sent back to the client
         * and the SMTP transaction will abort.
         *
         * @param message The received message.
         */
        void receiveMessage(SmtpMessage message);
    }

    private SmtpTransactionHandler(LineAwareBufferedInputStream input, PrintWriter output, SmtpFirewall firewall, Long maxMessageSize, MessageReceiver messageReceiver) {
        this.input = input;
        this.output = output;
        this.firewall = firewall;
        this.messageReceiver = messageReceiver;
        this.maxMessageSize = maxMessageSize;
    }

    /**
     * Handles the SMTP protocol communication.
     *
     * @param input           The input scanner.
     * @param output          The output writer.
     * @param firewall        The firewall.
     * @param maxMessageSize
     * @param messageReceiver The {@code MessageReceiver}.
     */
    public static void handle(LineAwareBufferedInputStream input, PrintWriter output, SmtpFirewall firewall, Long maxMessageSize, MessageReceiver messageReceiver) throws IOException, SmtpProtocolException {
        SmtpTransactionHandler sth = new SmtpTransactionHandler(input, output, firewall, maxMessageSize, messageReceiver);
        sth.execute();
    }

    private void execute() throws SmtpProtocolException {
        //inform the client about the SMTP server state
        reply(SmtpProtocolConstants.CODE_CONNECT, "localhost smtp4j server ready");

        //extends the EHLO/HELO command to greet the client
        SmtpCommand ehlo = SmtpCommand.parse(nextLine());
        if (ehlo != null) {
            if (ehlo.getType() == Type.EHLO) {
                String param = ehlo.getParameter();
                if (param == null || param.trim().isEmpty()) {
                    param = "you";
                }
                reply(SmtpProtocolConstants.CODE_OK, List.of(
                        "smtp4j greets " + param,
                        Type.EIGHT_BIT_MIME.withArgs(null),
                        Type.SIZE.withArgs(maxMessageSize != null ? maxMessageSize.toString() : "")
                ));
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
    private ByteArrayOutputStream smtpMessageContent;

    private final List<byte[]> readData = new ArrayList<>(64);
    private final List<SmtpExchange> exchanges = new ArrayList<>(32);

    private void readTransaction() throws SmtpProtocolException {
        boolean inForbiddenState = false;

        while (true) {
            SmtpCommand command = nextCommand();
            Type commandType = command.getType();

            if (inForbiddenState) {
                if (commandType == Type.QUIT) {
                    reply(SmtpProtocolConstants.CODE_OK, "OK");
                    break;
                }

                reply(SmtpProtocolConstants.CODE_FORBIDDEN, "Subsequent commands forbidden");
                continue;
            }

            if (mailFrom == null) {
                if (commandType == Type.MAIL_FROM) {
                    String enbraced = command.getParameter(); //enclosed: <mail_value>
                    mailFrom = enbraced.substring(1, enbraced.length() - 1);
                    if (!firewall.isAllowedFrom(mailFrom)) {
                        reply(SmtpProtocolConstants.CODE_FORBIDDEN, "Mail-From forbidden");
                        inForbiddenState = true;
                    } else {
                        reply(SmtpProtocolConstants.CODE_OK, "OK");
                    }
                } else if (commandType == Type.QUIT) {
                    reply(SmtpProtocolConstants.CODE_OK, "OK");
                    break;
                } else {
                    reply(SmtpProtocolConstants.CODE_BAD_COMMAND_SEQUENCE, "Bad sequence of command (wrong command)");
                }
                continue;
            }

            if (recipients == null) {
                if (commandType != Type.RECIPIENT) {
                    reply(SmtpProtocolConstants.CODE_BAD_COMMAND_SEQUENCE, "Bad sequence of command (wrong command)");
                    continue;
                }

                recipients = new ArrayList<>();
                while (commandType == Type.RECIPIENT) {
                    String enbraced = command.getParameter(); //enclosed: <mail_value>
                    String recipient = enbraced.substring(1, enbraced.length() - 1);
                    if (!firewall.isAllowedRecipient(recipient)) {
                        reply(SmtpProtocolConstants.CODE_FORBIDDEN, "Recipient forbidden");
                        inForbiddenState = true;
                    } else {
                        recipients.add(recipient);
                        reply(SmtpProtocolConstants.CODE_OK, "OK");
                    }

                    command = nextCommand();
                    commandType = command.getType();
                }
            }

            if (commandType == Type.DATA) {
                if (smtpMessageContent != null) {
                    reply(SmtpProtocolConstants.CODE_BAD_COMMAND_SEQUENCE, "Bad sequence of command (wrong command)");
                    continue;
                }

                smtpMessageContent = new ByteArrayOutputStream(8192);
                reply(SmtpProtocolConstants.CODE_INTERMEDIATE_REPLY, "Start mail input; end with <CRLF>.<CRLF>");

                boolean hasFailure = false;
                byte[] currentLine = nextLine();
                while (true) {
                    //DATA content must end with a dot on a single line
                    if (ByteArrayUtils.equals(currentLine, SmtpProtocolConstants.DOT_BYTES)) {
                        byte[] messageContent = ByteArrayUtils.copy(
                                smtpMessageContent.toByteArray(),
                                smtpMessageContent.size() - SmtpProtocolConstants.CRLF_BYTES.length);
                        if (!firewall.isAllowedMessage(messageContent)) {
                            reply(SmtpProtocolConstants.CODE_FORBIDDEN, "Message forbidden");
                            inForbiddenState = true;
                            hasFailure = true;
                            break;
                        }
                        SmtpMessage message = SmtpMessage.create(mailFrom, recipients, messageContent, new ArrayList<>(exchanges));
                        try {
                            messageReceiver.receiveMessage(message);
                            resetState();
                        } catch (Exception e) {
                            reply(SmtpProtocolConstants.CODE_TRANSACTION_FAILED, e.getMessage());
                            hasFailure = true;
                        }

                        break;
                    } else {
                        //if DATA starts with a dot, a second one must be added
                        if (ByteArrayUtils.startsWith(currentLine, SmtpProtocolConstants.DOT_BYTES)) {
                            smtpMessageContent.write(currentLine, 1, currentLine.length - 1);
                        } else {
                            smtpMessageContent.writeBytes(currentLine);
                        }
                        smtpMessageContent.writeBytes(SmtpProtocolConstants.CRLF_BYTES);
                    }

                    currentLine = nextLine();
                }

                if (!hasFailure) {
                    reply(SmtpProtocolConstants.CODE_OK, "OK");
                }
                continue;
            }

            if (commandType == Type.QUIT) {
                reply(SmtpProtocolConstants.CODE_OK, "OK");
                break;
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

    private byte[] nextLine() throws SmtpProtocolException {
        try {
            byte[] line = input.readLine();
            if (line == null) {
                throw new SmtpProtocolException("Unexpected end of stream (no more line)");
            }
            readData.add(line);
            return line;
        } catch (IOException ioe) {
            throw new SmtpProtocolException("I/O exception", ioe);
        }
    }

    private SmtpCommand nextCommand() throws SmtpProtocolException {
        SmtpCommand command = SmtpCommand.parse(nextLine());
        while (command != null) {
            Type commandType = command.getType();
            if (commandType == Type.NOOP) {
                reply(SmtpProtocolConstants.CODE_OK, "OK");
            } else if (commandType == Type.EXPAND) {
                reply(SmtpProtocolConstants.CODE_NOT_SUPPORTED, "Not supported");
            } else if (commandType == Type.VERIFY) {
                reply(SmtpProtocolConstants.CODE_NOT_SUPPORTED, "Not supported");
            } else if (commandType == Type.HELP) {
                reply(SmtpProtocolConstants.CODE_NOT_SUPPORTED, "Not supported");
            } else if (commandType == Type.UNKNOWN) {
                reply(SmtpProtocolConstants.CODE_COMMAND_UNKNOWN, "Unknown command");
            } else if (commandType == Type.RESET) {
                resetState();
                reply(SmtpProtocolConstants.CODE_OK, "OK");
            } else {
                return command;
            }

            command = SmtpCommand.parse(nextLine());
        }

        throw new SmtpProtocolException("Unexpected end of exchange (no more command)");
    }

    private void reply(int code, List<String> messages) {
        StringBuilder builder = new StringBuilder(32);
        for (int i = 0; i < messages.size(); i++) {
            boolean isLast = i + 1 == messages.size();

            String message = messages.get(i);
            builder.append(code);
            if (message != null) {
                if (isLast) {
                    builder.append(SmtpProtocolConstants.SP);
                } else {
                    builder.append(SmtpProtocolConstants.MULTILINE);
                }
                builder.append(message);
            } else {
                if (!isLast) {
                    builder.append(SmtpProtocolConstants.MULTILINE);
                }
            }
            builder.append(SmtpProtocolConstants.CRLF);
        }

        send(builder);
    }

    private void send(StringBuilder builder) {
        SmtpExchange exchange = new SmtpExchange(
                readData.stream()
                        .map(b -> new String(b, StandardCharsets.ISO_8859_1))
                        .toList(),
                builder.toString());
        exchanges.add(exchange);
        readData.clear();

        output.print(builder);
        output.flush();
    }

    private void reply(int code, String message) {
        StringBuilder builder = new StringBuilder(32);
        builder.append(code);
        if (message != null) {
            builder.append(SmtpProtocolConstants.SP);
            builder.append(message);
        }
        builder.append(SmtpProtocolConstants.CRLF);

        send(builder);
    }
}
