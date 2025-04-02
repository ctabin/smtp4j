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

/**
 * Represents a server listener.
 */
public interface SmtpServerListener {

    /**
     * Invoked when the {@code server} has been started and is ready to receive messages.
     *
     * @param server The started {@code SmtpServer}.
     */
    default void notifyStart(SmtpServer server) {}

    /**
     * Invoked when the {@code server} has been closed.
     *
     * @param server The stopped {@code SmtpServer}.
     */
    default void notifyClose(SmtpServer server) {}

    /**
     * Invoked when the {@code server} has received a message.
     * This method will be invoked within the background {@code Thread} used to receive
     * the message.
     *
     * @param server The {@code SmtpServer} that received the message.
     * @param smtpMessage The received message.
     */
    void notifyMessage(SmtpServer server, SmtpMessage smtpMessage);
}
