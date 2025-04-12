
package ch.astorm.smtp4j;

import ch.astorm.smtp4j.SmtpServerOptions.Protocol;
import ch.astorm.smtp4j.auth.LoginAuthenticationHandler;
import ch.astorm.smtp4j.auth.PlainAuthenticationHandler;
import ch.astorm.smtp4j.auth.SmtpAuthenticatorHandler;
import ch.astorm.smtp4j.core.SmtpMessageHandler;
import ch.astorm.smtp4j.core.SmtpServerListener;
import ch.astorm.smtp4j.secure.DefaultSSLContextProvider;
import ch.astorm.smtp4j.secure.SSLContextProvider;
import ch.astorm.smtp4j.store.SimpleUserRepository;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Helper to build a new {@code SmtpServer}.
 */
public class SmtpServerBuilder {
    private int port;
    private SmtpMessageHandler handler;
    private SmtpServerOptions options;
    private List<SmtpServerListener> listeners;
    private ThreadFactory threadFactory;

    /**
     * Defines the port on which the {@code SmtpServer} will listen to.
     * If the port is undefined, the server will make a dynamic lookup when it is started.
     *
     * @param port The port.
     * @return This builder.
     * @see SmtpServer#SmtpServer(int)
     */
    public SmtpServerBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Defines the SMTP server options..
     *
     * @param options The options.
     * @return This builder.
     * @see SmtpServer#setOptions(ch.astorm.smtp4j.SmtpServerOptions)
     */
    public SmtpServerBuilder withOptions(SmtpServerOptions options) {
        this.options = options;
        return this;
    }
    
    /**
     * Defines if the {@code STARTTLS} support is enabled (false by default).
     * If true, then a {@link #withSSLContextProvider(ch.astorm.smtp4j.secure.SSLContextProvider) SSL context provider}
     * must be set.
     *
     * @param startTlsSupport True if the {@code STARTTLS} support must be enabled.
     * @return This builder.
     */
    public SmtpServerBuilder withStartTLSSupport(boolean startTlsSupport) {
        if(options==null) { options = new SmtpServerOptions(); }
        options.starttls = startTlsSupport;
        return this;
    }

    /**
     * Defines the protocol to used.
     * If the {@link Protocol#SMTPS} is used, then a {@link #withSSLContextProvider(ch.astorm.smtp4j.secure.SSLContextProvider) SSL context provider}
     * must be set.
     *
     * @param protocol The protocol (by default {@link Protocol#SMTP}.)
     * @return This builder.
     */
    public SmtpServerBuilder withProtocol(Protocol protocol) {
        if(options==null) { options = new SmtpServerOptions(); }
        options.protocol = protocol;
        return this;
    }
    
    /**
     * Defines the {@link SSLContextProvider} to use when negotiating SSL.
     *
     * @param provider The provider.
     * @return This builder.
     * @see DefaultSSLContextProvider#selfSigned()
     */
    public SmtpServerBuilder withSSLContextProvider(SSLContextProvider provider) {
        if(options==null) { options = new SmtpServerOptions(); }
        options.sslContextProvider = provider;
        return this;
    }
    
    /**
     * Defines the {@link PrintStream} to use for debugging. If null, then no debug
     * output will be printed.
     *
     * @param debug The debug stream.
     * @return This builder.
     */
    public SmtpServerBuilder withDebugStream(PrintStream debug) {
        if(options==null) { options = new SmtpServerOptions(); }
        options.debug = debug;
        return this;
    }
    
    /**
     * Defines a custom connection string when a new client connects.
     *
     * @param str A simple connection answer or null.
     * @return This builder.
     */
    public SmtpServerBuilder withConnectionString(String str) {
        if(options==null) { options = new SmtpServerOptions(); }
        options.connectionString = str;
        return this;
    }

    /**
     * Adds the given {@code handler} to authenticate a client.
     * This method can be called multiple times to allow many authentication schemes.
     * Since the authentication will be required, you'll need to {@link #withUser(java.lang.String, java.lang.String) declare some users}.
     *
     * @param handler The authentication handler.
     * @return This builder.
     * @see PlainAuthenticationHandler#INSTANCE
     * @see LoginAuthenticationHandler#INSTANCE
     */
    public SmtpServerBuilder withAuthenticator(SmtpAuthenticatorHandler handler) {
        if(options==null) { options = new SmtpServerOptions(); }
        options.authenticators.add(handler);
        return this;
    }

    /**
     * Adds the given {@code user} to the repository.
     * This method can be called multiple times to add users.
     *
     * @param user The username.
     * @param password The associated password.
     * @return This builder.
     * @see SimpleUserRepository
     */
    public SmtpServerBuilder withUser(String user, String password) {
        if(options==null) { options = new SmtpServerOptions(); }
        options.usersRepository.addUser(user, password);
        return this;
    }

    /**
     * Defines the {@code SmtpMessageHandler} to be applied for the received messages.
     *
     * @param messageHandler The message handler.
     * @return This builder.
     * @see SmtpServer#SmtpServer(int, ch.astorm.smtp4j.core.SmtpMessageHandler, java.util.concurrent.ThreadFactory)
     */
    public SmtpServerBuilder withMessageHandler(SmtpMessageHandler messageHandler) {
        this.handler = messageHandler;
        return this;
    }
    
    /**
     * Defines the {@link ThreadFactory} to use to handle the SMTP messages.
     *
     * @param threadFactory The thread factory.
     * @return This builder.
     */
    public SmtpServerBuilder withThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    /**
     * Adds the specified {@code listener} once de server is build.
     *
     * @param listener The listener to add.
     * @return This builder.
     * @see SmtpServer#addListener(ch.astorm.smtp4j.core.SmtpServerListener)
     */
    public SmtpServerBuilder withListener(SmtpServerListener listener) {
        if(listeners==null) { listeners = new ArrayList<>(); }
        listeners.add(listener);
        return this;
    }

    /**
     * Builds the {@code SmtpServer}.
     *
     * @return A new {@code SmtpServer} instance.
     */
    public SmtpServer build() {
        SmtpServer server = new SmtpServer(port, handler, threadFactory);
        if(options!=null) { server.setOptions(options); }
        if(listeners!=null) { listeners.forEach(l -> server.addListener(l)); }
        return server;
    }

    /**
     * Builds the {@code SmtpServer} and starts it.
     *
     * @return A new {@code SmtpServer} instance.
     */
    public SmtpServer start() throws IOException {
        SmtpServer server = build();
        server.start();
        return server;
    }
}
