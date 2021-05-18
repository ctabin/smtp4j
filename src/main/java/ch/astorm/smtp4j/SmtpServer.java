
package ch.astorm.smtp4j;

import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.core.SmtpMessageHandler;
import ch.astorm.smtp4j.core.SmtpMessageStorage;
import ch.astorm.smtp4j.protocol.SmtpProtocolConstants;
import ch.astorm.smtp4j.protocol.SmtpTransactionHandler;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

/**
 * Simple SMTP server.
 * @see SmtpServerBuilder
 */
public class SmtpServer implements AutoCloseable {
    private int port;
    private final SmtpMessageStorage localStorage;

    private volatile SmtpMessageHandler messageHandler;
    private volatile ServerSocket serverSocket;
    private Thread localThread;

    /**
     * Default SMTP port.
     * The port 25 is generally used for a simple SMTP relay. Ports 587 and 2525 uses
     * explicit SSL/TLS connections whereas port 465 is for implicit SSL/TLS connections.
     * See <a href="https://www.sparkpost.com/blog/what-smtp-port/">here</a> for more information.
     */
    public static int DEFAULT_PORT = 25;

    /**
     * Creates a new {@code SmtpServer}.
     *
     * @param port The port to listen to. A value less or equal to zero indicates that
     *             a free port as to be discovered when the {@link #start() start} method
     *             is called.
     */
    public SmtpServer(int port) {
        this.port = port;
        this.localStorage = new SmtpMessageStorage();
        this.messageHandler = localStorage;
    }

    /**
     * Defines the {@code SmtpMessageHandler} that will receive all the incoming messages.
     * By default, all the messages are stored in a local {@link SmtpMessageStorage}.
     * <p>The {@code handler} will replace the current message handler that will not be
     * notified of received messages anymore. It is possible to set it to {@code null} to
     * restore default behavior.</p>
     *
     * @param handler The handler or null.
     */
    public void setMessageHandler(SmtpMessageHandler handler) {
        this.messageHandler = handler!=null ? handler : localStorage;
    }

    /**
     * Returns the current {@code SmtpMessageHandler}.
     *
     * @return The current message handler.
     */
    public SmtpMessageHandler getMessageHandler() {
        return messageHandler;
    }

    /**
     * Returns all the received messages.
     *
     * @return The received messages.
     * @see SmtpMessageStorage#getMessages()
     */
    public List<SmtpMessage> getReceivedMessages() {
        synchronized(localStorage) {
            return localStorage.getMessages();
        }
    }

    /**
     * Clears all the received messages.
     * @see SmtpMessageStorage#clear()
     */
    public void clearReceivedMessages() {
        synchronized(localStorage) {
            localStorage.clear();
        }
    }

    /**
     * Returns the port on which the {@code SmtpServer} listen to.
     * If the value is zero or less, then the port will be discovered when the server
     * is {@link #start() started}.
     *
     * @return The port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns true if the server has been closed or is not started yet.
     *
     * @return True if the server has been closed or is not started yet.
     */
    public boolean isClosed() {
        return serverSocket==null;
    }

    /**
     * Starts the server.
     * If the server is already started, this method will raise and {@code IllegalStateException}.
     */
    public synchronized void start() throws IOException {
        if(!isClosed()) { throw new IllegalStateException("Server already started"); }

        if(port<=0) {
            //by default, try with the default SMTP port
            serverSocket = createSocketIfPossible(DEFAULT_PORT);
            if(serverSocket!=null) { port = DEFAULT_PORT; }
            else {
                //generally, ports below 1024 are restricted to root
                //so we directly start here to maximize chances to find an open port
                int currentPort = 1024;
                while(serverSocket==null && currentPort<65536) {
                    serverSocket = createSocketIfPossible(currentPort);
                    if(serverSocket!=null) { port = currentPort; }
                    ++currentPort;
                }
            }

            if(serverSocket==null) {
                throw new IOException("Unable to start SMTP server (no free port found)");
            }
        } else {
            //creates manually the socket here, so in case of error we can have the
            //source IOException raised
            serverSocket = new ServerSocket(port);
        }

        localThread = new Thread(new SmtpPacketListener());
        localThread.start();
    }

    private ServerSocket createSocketIfPossible(int port) {
        try { return new ServerSocket(port); }
        catch(IOException e) { return null; }
    }

    /**
     * Closes this {@code SmtpServer} instance and releases all the resources associated
     * to it. Once closed, it possible to restart it again.
     * If the server is already closed, this method does nothing.
     */
    @Override
    public synchronized void close() throws IOException {
        if(isClosed()) { return; } //already closed

        ServerSocket localServerSocket = serverSocket;
        serverSocket = null;

        //will trigger a I/O exception in the running thread
        localServerSocket.close();

        try { localThread.join(); }
        catch(InterruptedException ie) { /* ignored */ }
        localThread = null;
    }

    private class SmtpPacketListener implements Runnable {
        @Override
        public void run() {
            while(serverSocket!=null) {
                try(Socket socket = serverSocket.accept();
                    Scanner input = new Scanner(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1)).useDelimiter(SmtpProtocolConstants.CRLF);
                    PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1))) {
                    synchronized(localStorage) { SmtpTransactionHandler.handle(socket, input, output, messageHandler); }
                } catch(IOException ioe) {
                    /* ignored */
                }
            }
        }
    }
}
