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
import ch.astorm.smtp4j.core.SmtpMessageHandler;
import ch.astorm.smtp4j.core.SmtpServerListener;
import ch.astorm.smtp4j.firewall.AllowAllSmtpFirewall;
import ch.astorm.smtp4j.firewall.SmtpFirewall;
import ch.astorm.smtp4j.secure.SmtpSecure;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Helper to build a new {@code SmtpServer}.
 */
public class SmtpServerBuilder {
    private int port;
    private SmtpMessageHandler handler;
    private List<SmtpServerListener> listeners;
    private ExecutorService executorService;
    private Duration socketTimeout;
    private Long maxMessageSize;
    private SmtpFirewall firewall = AllowAllSmtpFirewall.INSTANCE;
    private SmtpAuth auth;
    private SmtpSecure secure;

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
     * Defines the {@code SmtpMessageHandler} to be applied for the received messages.
     *
     * @param messageHandler The message handler.
     * @return This builder.
     * @see SmtpServer#SmtpServer(int, SmtpMessageHandler, ExecutorService, Duration, Long, SmtpFirewall, SmtpAuth, SmtpSecure)
     */
    public SmtpServerBuilder withMessageHandler(SmtpMessageHandler messageHandler) {
        this.handler = messageHandler;
        return this;
    }

    /**
     * Defines the {@link ExecutorService} to use to handle the SMTP messages.
     *
     * @param executorService The executor service.
     * @return This builder.
     */
    public SmtpServerBuilder withExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
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
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
        return this;
    }

    /**
     * Sets the socket timeout duration for the {@code SmtpServer}.
     *
     * @param socketTimeout The duration of the socket timeout.
     * @return This builder, to allow method chaining.
     */
    public SmtpServerBuilder withSocketTimeout(Duration socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    /**
     * Sets the maximum message size for the {@code SmtpServer}.
     *
     * @param maxMessageSize The maximum allowed size for a message, in bytes.
     * @return This builder, to allow method chaining.
     */
    public SmtpServerBuilder withMaxMessageSize(long maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
        return this;
    }

    /**
     * Sets the {@code SmtpFirewall} to be used by the {@code SmtpServer}.
     *
     * @param firewall The firewall configuration to use for the server.
     * @return This builder, to allow method chaining.
     */
    public SmtpServerBuilder withFirewall(SmtpFirewall firewall) {
        this.firewall = Objects.requireNonNullElse(firewall, AllowAllSmtpFirewall.INSTANCE);
        return this;
    }

    /**
     * Sets the authentication mechanism for the {@code SmtpServer}.
     *
     * @param auth The authentication implementation to use.
     * @return This builder, to allow method chaining.
     */
    public SmtpServerBuilder withAuth(SmtpAuth auth) {
        this.auth = auth;
        return this;
    }

    /**
     * Configures the SMTP server to use the specified security settings.
     *
     * @param secure The security configuration to use for the server.
     * @return This builder, to allow method chaining.
     */
    public SmtpServerBuilder withSecure(SmtpSecure secure) {
        this.secure = secure;
        return this;
    }

    /**
     * Builds the {@code SmtpServer}.
     *
     * @return A new {@code SmtpServer} instance.
     */
    public SmtpServer build() {
        SmtpServer server = new SmtpServer(port, handler, executorService, socketTimeout, maxMessageSize, firewall, auth, secure);
        if (listeners != null) {
            listeners.forEach(server::addListener);
        }
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
