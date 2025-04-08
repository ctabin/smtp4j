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

package ch.astorm.smtp4j.util;

import ch.astorm.smtp4j.SmtpServer;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketTracker {
    private static final Logger LOG = Logger.getLogger(SmtpServer.class.getName());

    private final Set<Socket> connectedSockets = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    public void registerSocket(Socket socket) {
        connectedSockets.add(socket);
    }

    public void unregisterSocket(Socket socket) {
        connectedSockets.remove(socket);
    }

    public void close() {
        for (Iterator<Socket> iterator = connectedSockets.iterator(); iterator.hasNext(); ) {
            Socket socket = iterator.next();
            iterator.remove();

            try {
                socket.close();
            } catch (IOException e) {
                LOG.log(Level.FINER, "Error closing socket", e);
            }
        }
    }
}
