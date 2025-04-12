
package ch.astorm.smtp4j.protocol;

import ch.astorm.smtp4j.SmtpServer;
import ch.astorm.smtp4j.SmtpServerOptions;
import ch.astorm.smtp4j.SmtpServerOptions.Protocol;
import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.protocol.SmtpCommand.Type;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Handles the SMTP protocol.
 */
public class SmtpTransactionHandler implements AutoCloseable {
    private final SmtpServerOptions options;
    private final MessageReceiver messageReceiver;

    private boolean secureChannel;
    private Socket socket;
    private InputStream socketInputStream;
    private OutputStream socketOutputStream;
    private BufferedReader input;
    private PrintWriter output;
    
    /**
     * Represents a message receiver within the SMTP transaction.
     */
    @FunctionalInterface
    public static interface MessageReceiver {

        /**
         * Invoked when a message is received.
         * If this method throws an exception, the error will be sent back to the client
         * and the SMTP transaction will abort.
         *
         * @param message The received message.
         */
        void receiveMessage(SmtpMessage message);
    }

    private SmtpTransactionHandler(SmtpServer smtpServer, Socket socket, MessageReceiver messageReceiver) throws IOException {
        this.options = smtpServer.getOptions();
        this.messageReceiver = messageReceiver;
        initSocket(socket, false);
    }

    private void initSocket(Socket socket, boolean sslSocket) throws IOException {
        this.secureChannel = sslSocket;
        this.socket = socket;
        this.socketInputStream = socket.getInputStream();
        this.socketOutputStream = socket.getOutputStream();
        this.input = new BufferedReader(new InputStreamReader(socketInputStream, StandardCharsets.US_ASCII));
        this.output = new PrintWriter(new OutputStreamWriter(socketOutputStream, StandardCharsets.US_ASCII));
    }
    
    /**
     * Handles the SMTP protocol communication.
     *
     * @param smtpServer The SMTP server.
     * @param socket The Socket.
     * @param messageReceiver The {@code MessageReceiver}.
     */
    public static void handle(SmtpServer smtpServer, Socket socket, MessageReceiver messageReceiver) throws IOException, SmtpProtocolException {
        try(SmtpTransactionHandler sth = new SmtpTransactionHandler(smtpServer, socket, messageReceiver)) {
            sth.execute();
        }
    }

    @Override
    public void close() throws IOException {
        input.close();
        output.close();
        socket.close();
    }
    
    private void execute() throws SmtpProtocolException {
        if(!secureChannel) {
            if(options.protocol==Protocol.SMTPS) {
                try { upgradeToTLSSocket(); }
                catch(Exception e) { throw new SmtpProtocolException("TLS Upgrade failed (SMTPS)", e); }

                reply(SmtpProtocolConstants.CODE_CONNECT, "localhost smtp4j server ready (SMPTS)");

                execute();
                return;
            }

            //inform the client about the SMTP server state
            reply(SmtpProtocolConstants.CODE_CONNECT, "localhost smtp4j server ready");
        }

        //extends the EHLO/HELO command to greet the client
        boolean supportsStartTls = options.starttls && !secureChannel;
        SmtpCommand ehlo = SmtpCommand.parse(nextLine());
        if(ehlo!=null) {
            if(ehlo.getType()==Type.EHLO) {
                String param = ehlo.getParameter();
                String greetings = param!=null ? "smtp4j greets "+ehlo.getParameter() : "OK";
                
                List<String> replies = new ArrayList<>();
                replies.add(greetings);
                if(supportsStartTls) { replies.add("STARTTLS"); }
                
                reply(SmtpProtocolConstants.CODE_OK, replies);
            } else {
                reply(SmtpProtocolConstants.CODE_BAD_COMMAND_SEQUENCE, "Bad sequence of command (wrong command)");
                return;
            }
        } else {
            reply(SmtpProtocolConstants.CODE_BAD_COMMAND_SEQUENCE, "Bad sequence of command (no more token)");
            return;
        }

        if(supportsStartTls) {
            SmtpCommand startTTLS = nextCommand();
            if(startTTLS.getType()==Type.STARTTLS) {
                PrintWriter plainStream = output;
                try {
                    upgradeToTLSSocket();
                } catch(Exception e) {
                    reply(SmtpProtocolConstants.CODE_TRANSACTION_FAILED, "TLS Upgrade failed");
                    throw new SmtpProtocolException("TLS Upgrade failed", e);
                }

                reply(plainStream, SmtpProtocolConstants.CODE_CONNECT, "Go ahead", SmtpProtocolConstants.SP_FINAL);
                execute();
                return;
            } else {
                //it is not a STARTTLS command, stack it for the transaction
                stackedCommands.add(startTTLS);
            }
        }
        
        //start reading the transaction data
        readTransaction();
    }

    private void upgradeToTLSSocket() throws Exception {
        if(options.sslContextProvider==null) { throw new IllegalStateException("No SSLContextProvider defined"); }

        SSLContext sslContext = options.sslContextProvider.getSSLContext();
        if(sslContext==null) { throw new IllegalStateException("SSLContext is null"); }

        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket)sslSocketFactory.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
        sslSocket.setUseClientMode(false);

        this.initSocket(sslSocket, true);
    }

    private String mailFrom;
    private List<String> recipients;
    private StringBuilder smtpMessageContent;
    
    private final List<String> readData = new ArrayList<>(64);
    private final List<SmtpExchange> exchanges = new ArrayList<>(32);

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
                    reply(SmtpProtocolConstants.CODE_QUIT, "goodbye");
                    break;
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
                reply(SmtpProtocolConstants.CODE_INTERMEDIATE_REPLY, "Start mail input; end with <CRLF>.<CRLF>");

                boolean hasFailure = false;
                String currentLine = nextLine();
                while(currentLine!=null) {
                    //DATA content must end with a dot on a single line
                    if(currentLine.equals(SmtpProtocolConstants.DOT)) {
                        smtpMessageContent.delete(smtpMessageContent.length()-SmtpProtocolConstants.CRLF.length(), smtpMessageContent.length());
                        SmtpMessage message = SmtpMessage.create(mailFrom, recipients, smtpMessageContent.toString(), new ArrayList<>(exchanges));
                        try {
                            messageReceiver.receiveMessage(message);
                            resetState();
                        } catch(Exception e) {
                            reply(SmtpProtocolConstants.CODE_TRANSACTION_FAILED, e.getMessage());
                            hasFailure = true;
                        }
                        
                        break;
                    } else {
                        //if DATA starts with a dot, a second one must be added
                        if(currentLine.startsWith(SmtpProtocolConstants.DOT)) { currentLine = currentLine.substring(1); }
                        smtpMessageContent.append(currentLine).append(SmtpProtocolConstants.CRLF);
                    }

                    currentLine = nextLine();
                }

                if(!hasFailure) { reply(SmtpProtocolConstants.CODE_OK, "OK"); }
                continue;
            }

            if(commandType==Type.QUIT) {
                reply(SmtpProtocolConstants.CODE_QUIT, "goodbye");
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

    private String nextLine() throws SmtpProtocolException {
        try {
            String line = input.readLine();
            if(line==null) { throw new SmtpProtocolException("Unexpected end of stream (no more line)"); }
            readData.add(line);
            if(options.debug!=null) { options.debug.println("> "+line.trim()); }
            return line;
        } catch(IOException ioe) {
            throw new SmtpProtocolException("I/O exception", ioe);
        }
    }
    
    private final List<SmtpCommand> stackedCommands = new ArrayList<>();
    private SmtpCommand nextCommand() throws SmtpProtocolException {
        SmtpCommand command = stackedCommands.isEmpty() ? SmtpCommand.parse(nextLine()) : stackedCommands.remove(0);
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
        reply(code, message, SmtpProtocolConstants.SP_FINAL);
    }
    
    private void reply(int code, List<String> messages) {
        for(int i=0 ;i<messages.size()-1 ; ++i) {
            reply(code, messages.get(i), SmtpProtocolConstants.SP_CONTINUE);
        }
        reply(code, messages.get(messages.size()-1), SmtpProtocolConstants.SP_FINAL);
    }
    
    private void reply(int code, String message, String separator) {
        reply(output, code, message, separator);
    }
    
    private void reply(PrintWriter stream, int code, String message, String separator) {
        StringBuilder builder = new StringBuilder(32);
        builder.append(code);
        if(message!=null) {
            builder.append(separator);
            builder.append(message);
        }
        builder.append(SmtpProtocolConstants.CRLF);

        SmtpExchange exchange = new SmtpExchange(new ArrayList<>(readData), builder.toString());
        exchanges.add(exchange);
        readData.clear();
        
        if(options.debug!=null) { options.debug.println("< "+builder.toString().trim()); }
        stream.print(builder.toString());
        stream.flush();
    }
}
