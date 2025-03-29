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

package ch.astorm.smtp4j;

import ch.astorm.smtp4j.auth.SmtpAuth;
import ch.astorm.smtp4j.core.DefaultSmtpMessageHandler;
import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.core.SmtpMessageHandler;
import ch.astorm.smtp4j.core.SmtpMessageHandler.SmtpMessageReader;
import ch.astorm.smtp4j.core.SmtpServerListener;
import ch.astorm.smtp4j.firewall.AllowAllSmtpFirewall;
import ch.astorm.smtp4j.firewall.SmtpFirewall;
import ch.astorm.smtp4j.protocol.SmtpProtocolException;
import ch.astorm.smtp4j.protocol.SmtpTransactionHandler;
import ch.astorm.smtp4j.util.CloseableReentrantLock;
import ch.astorm.smtp4j.util.LineAwareBufferedInputStream;
import ch.astorm.smtp4j.util.MaxMessageSizeInputStream;
import jakarta.mail.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple SMTP server.
 *
 * @see SmtpServerBuilder
 */
public class SmtpServer implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(SmtpServer.class.getName());

    private final CloseableReentrantLock thisLock = new CloseableReentrantLock();
    private int port;
    private final SmtpMessageHandler messageHandler;
    private final List<SmtpServerListener> listeners;
    private final ExecutorService executorService;
    private final Duration socketTimeout;
    private final Long maxMessageSize;
    private final SmtpFirewall firewall;
    private final SmtpAuth auth;

    private volatile ServerSocket serverSocket;
    private Future<?> serverThread;

    /**
     * Default SMTP port.
     * The port 25 is generally used for a simple SMTP relay. Ports 587 and 2525 uses
     * explicit SSL/TLS connections whereas port 465 is for implicit SSL/TLS connections.
     * See <a href="https://www.sparkpost.com/blog/what-smtp-port/">here</a> for more information.
     */
    public static int DEFAULT_PORT = 25;

    /**
     * Creates a new {@code SmtpServer} with a {@link DefaultSmtpMessageHandler} instance to
     * handle received messages.
     *
     * @param port The port to listen to. A value less or equal to zero indicates that
     *             a free port as to be discovered when the {@link #start() start} method
     *             is called.
     */
    public SmtpServer(int port) {
        this(port, null, null, null, null, AllowAllSmtpFirewall.INSTANCE, null);
    }

    /**
     * Creates a new {@code SmtpServer}.
     * The {@code messageHandler} will always be notified first for the {@link SmtpServerListener}
     * events and is NOT part of the {@link #getListeners() listeners} list.
     *
     * @param port            The port to listen to. A value less or equal to zero indicates that
     *                        a free port as to be discovered when the {@link #start() start} method
     *                        is called.
     * @param messageHandler  The {@code SmtpMessageHandler} used to receive messages or null to
     *                        use a new {@link DefaultSmtpMessageHandler} instance.
     * @param executorService The {@link ExecutorService} to use.
     * @param socketTimeout   The socket timeout duration for any network communication.
     * @param maxMessageSize  The maximum message size before the handler closes the connection
     * @param firewall
     * @param auth
     */
    public SmtpServer(int port, SmtpMessageHandler messageHandler, ExecutorService executorService, Duration socketTimeout, Long maxMessageSize, SmtpFirewall firewall, SmtpAuth auth) {
        this.port = port;
        this.messageHandler = messageHandler != null ? messageHandler : new DefaultSmtpMessageHandler();
        this.executorService = executorService != null ? executorService : Executors.newWorkStealingPool();
        this.listeners = new ArrayList<>(4);
        this.socketTimeout = socketTimeout;
        this.maxMessageSize = maxMessageSize;
        this.firewall = firewall;
        this.auth = auth;
    }

    /**
     * Returns the basic {@code Properties} that can be used for {@link Session}.
     * If the port is dynamic, then the server must have been started before this
     * method can be called.
     *
     * @return The properties for this server.
     */
    public Properties getSessionProperties() {
        if (port <= 0) {
            throw new IllegalStateException("Dynamic port lookup: server must be started");
        }

        Properties props = new Properties();
        props.setProperty("mail.smtp.host", "localhost");
        props.setProperty("mail.smtp.port", "" + port);
        return props;
    }

    /**
     * Creates a new {@code Session} instance that will send messages to this server.
     *
     * @return A new {@code Session} instance.
     */
    public Session createSession() {
        return Session.getInstance(getSessionProperties());
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
     * Returns a new {@link SmtpMessageReader} to read incoming messages.
     *
     * @return A new {@code SmtpMessageReader} instance.
     * @see SmtpMessageHandler#messageReader()
     */
    public SmtpMessageReader receivedMessageReader() {
        return messageHandler.messageReader();
    }

    /**
     * Returns all the (newly) received messages.
     * If no message has been received since the last invocation, an empty list
     * will be returned.
     * <p>Note that if a {@link #receivedMessageReader() reader} has been created, this
     * method will compete over the same list, hence the messages returned won't be received
     * through the reader.</p>
     * <p>In case there is no message, this method will wait 200 milliseconds before
     * returning to let a chance for any new message to arrive.</p>
     *
     * @return A list with the newly received messages or an empty list.
     * @see SmtpMessageHandler#readMessages(long, java.util.concurrent.TimeUnit)
     */
    public List<SmtpMessage> readReceivedMessages() {
        return readReceivedMessages(200, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns all the (newly) received messages.
     * If no message has been received since the last invocation, an empty list
     * will be returned.
     * <p>Note that if a {@link #receivedMessageReader() reader} has been created, this
     * method will compete over the same list, hence the messages returned won't be received
     * through the reader.</p>
     *
     * @param delayIfNoMessage Delay to wait if there is no message or a negative value to return immediately.
     * @param unit             The time unit of {@code delayIfNoMessage}.
     * @return A list with the newly received messages or an empty list.
     * @see SmtpMessageHandler#readMessages(long, java.util.concurrent.TimeUnit)
     */
    public List<SmtpMessage> readReceivedMessages(long delayIfNoMessage, TimeUnit unit) {
        return messageHandler.readMessages(delayIfNoMessage, unit);
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
     * Returns true if the {@code SmtpServer} is started and is actually listening for
     * new messages.
     *
     * @return True if the server has been {@link #start() started} and not yet closed.
     */
    public boolean isRunning() {
        return serverSocket != null;
    }

    /**
     * Returns true if the server has been closed or is not started yet.
     *
     * @return True if the server has been closed or is not started yet.
     */
    public boolean isClosed() {
        return serverSocket == null;
    }

    /**
     * Starts the server.
     * If the server is already started, this method will raise and {@code IllegalStateException}.
     */
    public void start() throws IOException {
        try (var _ = thisLock.lockCloseable()) {
            if (!isClosed()) {
                throw new IllegalStateException("Server already started");
            }

            if (port <= 0) {
                //by default, try with the default SMTP port
                serverSocket = createSocketIfPossible(DEFAULT_PORT);
                if (serverSocket != null) {
                    port = DEFAULT_PORT;
                } else {
                    //generally, ports below 1024 are restricted to root,
                    //so we directly start here to maximize chances to find an open port
                    int currentPort = 1024;
                    while (serverSocket == null && currentPort < 65536) {
                        serverSocket = createSocketIfPossible(currentPort);
                        if (serverSocket != null) {
                            port = currentPort;
                        }
                        ++currentPort;
                    }
                }

                if (serverSocket == null) {
                    throw new IOException("Unable to start SMTP server (no free port found)");
                }
            } else {
                //manually creates the socket here, so in case of error we can have the
                //source IOException raised
                serverSocket = new ServerSocket(port);
            }

            this.serverThread = executorService.submit(new SmtpPacketListener());

            notifyStarted();
        }
    }

    private ServerSocket createSocketIfPossible(int port) {
        try {
            return new ServerSocket(port);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Registers the specified {@code listener} to the server's events.
     *
     * @param listener The listener to add.
     */
    public void addListener(SmtpServerListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes the specified {@code listener} of the server's event notifications.
     *
     * @param listener The listener to remove.
     * @return True if the listener has been removed.
     */
    public boolean removeListener(SmtpServerListener listener) {
        return this.listeners.remove(listener);
    }

    /**
     * Returns the listeners of this server.
     * The returned list is live.
     *
     * @return The listeners.
     */
    public List<SmtpServerListener> getListeners() {
        return this.listeners;
    }

    private void notifyStarted() {
        messageHandler.notifyStart(this);
        listeners.forEach(l -> l.notifyStart(this));
    }

    private void notifyClosed() {
        messageHandler.notifyClose(this);
        listeners.forEach(l -> l.notifyClose(this));
    }

    private void notifyMessage(SmtpMessage message) {
        messageHandler.notifyMessage(this, message);
        listeners.forEach(l -> l.notifyMessage(this, message));
    }

    /**
     * Closes this {@code SmtpServer} instance and releases all the resources associated
     * to it. Once closed, it possible to restart it again.
     * If the server is already closed, this method does nothing.
     */
    @Override
    public void close() throws IOException {
        try (var _ = thisLock.lockCloseable()) {
            if (isClosed()) {
                return;
            } //already closed

            ServerSocket localServerSocket = serverSocket;
            serverSocket = null;

            //will trigger a I/O exception in the running thread
            localServerSocket.close();

            serverThread.cancel(true);
            while (!serverThread.isDone()) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            serverThread = null;

            notifyClosed();
        }
    }

    private class SmtpPacketListener implements Runnable {
        @Override
        public void run() {
            while (serverSocket != null) {
                try {
                    Socket socket = serverSocket.accept();
                    if (!firewall.accept(socket.getInetAddress())) {
                        socket.close();
                    }

                    LOG.log(Level.FINER, "Got connection from " + socket.getRemoteSocketAddress());
                    executorService.submit(() -> handleConnection(socket));
                } catch (IOException e) {
                    /* can be generally safely ignored because occurs when the server is being closed */
                    LOG.log(Level.FINER, "I/O Exception", e);
                }
            }
        }

        private void handleConnection(Socket socket) {
            try (socket;
                 LineAwareBufferedInputStream input = new LineAwareBufferedInputStream(
                         firewall.firewallInputStream(
                                 wrapMaxMessageSizeStream(socket.getInputStream())));
                 PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII))) {

                if (socketTimeout != null) {
                    try {
                        socket.setSoTimeout((int) socketTimeout.toMillis());
                    } catch (SocketException e) {
                        LOG.log(Level.SEVERE, "Could not set socket timeout", e);
                    }
                }

                SmtpTransactionHandler.handle(input, output, firewall, maxMessageSize, auth, SmtpServer.this::notifyMessage);
            } catch (SmtpProtocolException spe) {
                LOG.log(Level.WARNING, "Protocol Exception", spe);
            } catch (IOException ioe) {
                /* can be generally safely ignored because occurs when the server is being closed */
                LOG.log(Level.FINER, "I/O Exception", ioe);
            }
        }

        private InputStream wrapMaxMessageSizeStream(InputStream inputStream) {
            if (maxMessageSize == null) {
                return inputStream;
            }

            return new MaxMessageSizeInputStream(maxMessageSize, inputStream);
        }
    }
}
