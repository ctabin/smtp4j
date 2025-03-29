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
package ch.astorm.smtp4j.core;

import ch.astorm.smtp4j.SmtpServer;
import ch.astorm.smtp4j.util.CloseableReentrantLock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Simple {@code SmtpMessageHandler} that stores the received messages in a list.
 * This class is Thread-safe.
 */
public class DefaultSmtpMessageHandler implements SmtpMessageHandler {

    private final CloseableReentrantLock messagesLock = new CloseableReentrantLock();
    private final List<SmtpMessage> messages = new ArrayList<>(32);
    private volatile boolean serverStarted;

    @Override
    public void notifyStart(SmtpServer server) {
        serverStarted = true;
    }

    @Override
    public void notifyClose(SmtpServer server) {
        try (var _ = messagesLock.lockCloseable()) {
            serverStarted = false;
            messagesLock.notifyCondition();
        }
    }

    @Override
    public void notifyMessage(SmtpServer server, SmtpMessage smtpMessage) {
        try (var _ = messagesLock.lockCloseable()) {
            messages.add(smtpMessage);
            messagesLock.notifyCondition();
        }
    }

    @Override
    public SmtpMessageReader messageReader() {
        return new SmtpMessageReader() {
            private List<SmtpMessage> localMessages = List.of();

            @Override
            public SmtpMessage readMessage() {
                if (!localMessages.isEmpty()) {
                    return localMessages.removeFirst();
                }

                try (var _ = messagesLock.lockCloseable()) {
                    do {
                        localMessages = readMessages(1, TimeUnit.SECONDS);
                    } while (serverStarted && localMessages.isEmpty());
                }

                return !localMessages.isEmpty() ? localMessages.removeFirst() : null;
            }

            @Override
            public void close() {
                /* nothing */
            }
        };
    }

    @Override
    public List<SmtpMessage> readMessages(long delayIfNoMessage, TimeUnit unit) {
        if (!serverStarted) {
            return List.of();
        }

        try (var _ = messagesLock.lockCloseable()) {
            if (messages.isEmpty() && delayIfNoMessage >= 0) {
                try {
                    messagesLock.awaitCondition(delayIfNoMessage, unit);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }

            if (messages.isEmpty()) {
                return List.of();
            }

            List<SmtpMessage> copyMsgs = new ArrayList<>(messages);
            messages.clear();
            return copyMsgs;
        }
    }
}
