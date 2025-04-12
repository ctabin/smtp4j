
package ch.astorm.smtp4j;

import ch.astorm.smtp4j.SmtpServerOptions.Protocol;
import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.core.SmtpMessageHandler;
import ch.astorm.smtp4j.core.SmtpMessageHandler.SmtpMessageReader;
import ch.astorm.smtp4j.core.DefaultSmtpMessageHandler;
import ch.astorm.smtp4j.core.SmtpServerListener;
import ch.astorm.smtp4j.protocol.SmtpProtocolException;
import ch.astorm.smtp4j.protocol.SmtpTransactionHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple SMTP server.
 * @see SmtpServerBuilder
 */
public class SmtpServer implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(SmtpServer.class.getName());

    private int port;
    private final SmtpMessageHandler messageHandler;
    private final ReentrantLock messageHandlerLock;
    private final List<SmtpServerListener> listeners;
    private final ThreadFactory threadFactory;
    
    private volatile SmtpServerOptions serverOptions;
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
     * Creates a new {@code SmtpServer} with a {@link DefaultSmtpMessageHandler} instance to
     * handle received messages.
     *
     * @param port The port to listen to. A value less or equal to zero indicates that
     *             a free port as to be discovered when the {@link #start() start} method
     *             is called.
     */
    public SmtpServer(int port) {
        this(port, null, null);
    }

    /**
     * Creates a new {@code SmtpServer}.
     * The {@code messageHandler} will always be notified first for the {@link SmtpServerListener}
     * events and is NOT part of the {@link #getListeners() listeners} list.
     *
     * @param port The port to listen to. A value less or equal to zero indicates that
     *             a free port as to be discovered when the {@link #start() start} method
     *             is called.
     * @param messageHandler The {@code SmtpMessageHandler} used to receive messages or null to
     *             use a new {@link DefaultSmtpMessageHandler} instance.
     * @param threadFactory The {@link ThreadFactory} to use.
     */
    public SmtpServer(int port, SmtpMessageHandler messageHandler, ThreadFactory threadFactory) {
        this.port = port;
        this.messageHandler = messageHandler!=null ? messageHandler : new DefaultSmtpMessageHandler();
        this.messageHandlerLock = new ReentrantLock();
        this.threadFactory = threadFactory!=null ? threadFactory : Executors.defaultThreadFactory();
        this.listeners = new ArrayList<>(4);
        this.serverOptions = new SmtpServerOptions();
    }

    /**
     * Returns the SMTP server options.
     *
     * @return The options.
     */
    public SmtpServerOptions getOptions() {
        return serverOptions;
    }
    
    /**
     * Sets the SMTP options.
     *
     * @param options The options, cannot be null.
     */
    public void setOptions(SmtpServerOptions options) {
        if(options==null) { throw new IllegalArgumentException("options not defined"); }
        this.serverOptions = options;
    }
    
    /**
     * Returns the basic {@code Properties} that can be used for {@link Session}.
     * If the port is dynamic, then the server must have been started before this
     * method can be called.
     * 
     * @return The properties for this server.
     */
    public Properties getSessionProperties() {
        if(port<=0) { throw new IllegalStateException("Dynamic port lookup: server must be started"); }

        String protocol = serverOptions.protocol.name().toLowerCase();

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", protocol);
        props.setProperty("mail.transport.protocol.rfc822", protocol);
        props.setProperty("mail."+protocol+".host", "localhost");
        props.setProperty("mail."+protocol+".port", ""+port);
        if(serverOptions.starttls) {
            props.put("mail."+protocol+".starttls.enable", "true");
            props.put("mail."+protocol+".starttls.required", "true");
        }
        if(serverOptions.starttls || serverOptions.protocol==Protocol.SMTPS) {
            props.put("mail."+protocol+".ssl.checkserveridentity", "false");
            props.put("mail."+protocol+".ssl.trust", "*");
        }
        if(serverOptions.authenticators!=null && !serverOptions.authenticators.isEmpty()) {
            props.put("mail."+protocol+".auth", "true");
        }
        return props;
    }
    
    /**
     * Creates a new {@code Session} instance that will send messages to this server.
     * This session has won't authenticate any user.
     * 
     * @return A new {@code Session} instance.
     * @see #getSessionProperties()
     * @see #createAuthenticatedSession(java.lang.String, java.lang.String)
     */
    public Session createSession() {
        return Session.getInstance(getSessionProperties());
    }

    /**
     * Creates a new {@code Session} instance that will send messages to this server
     * which provides a user for the authentication.
     *
     * @param username The username.
     * @param password The password.
     * @return A new {@code Session} instance.
     * @see #getSessionProperties()
     */
    public Session createAuthenticatedSession(String username, String password) {
        return Session.getInstance(getSessionProperties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
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
     * @param unit The time unit of {@code delayIfNoMessage}.
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
        return serverSocket!=null;
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
    public void start() throws IOException {
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

        localThread = threadFactory.newThread(new SmtpPacketListener());
        localThread.start();

        messageHandlerLock.lock();
        try { notifyStarted(); }
        finally { messageHandlerLock.unlock(); }
    }

    private ServerSocket createSocketIfPossible(int port) {
        try { return new ServerSocket(port); }
        catch(IOException e) { return null; }
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
        if(isClosed()) { return; } //already closed

        ServerSocket localServerSocket = serverSocket;
        serverSocket = null;

        //will trigger a I/O exception in the running thread
        localServerSocket.close();

        try { localThread.join(); }
        catch(InterruptedException ie) { /* ignored */ }
        localThread = null;

        messageHandlerLock.lock();
        try { notifyClosed(); }
        finally { messageHandlerLock.unlock(); }
    }

    private class SmtpPacketListener implements Runnable {
        @Override
        public void run() {
            while(serverSocket!=null) {
                try(Socket socket = serverSocket.accept()) {
                    messageHandlerLock.lock();
                    try { SmtpTransactionHandler.handle(SmtpServer.this, socket, m -> notifyMessage(m)); }
                    finally { messageHandlerLock.unlock(); }
                } catch(SmtpProtocolException spe) {
                    LOG.log(Level.WARNING, "Protocol Exception", spe);
                } catch(IOException ioe) {
                    /* can be generally safely ignored because occurs when the server is being closed */
                    LOG.log(Level.FINER, "I/O Exception", ioe);
                }
            }
        }
    }
}
