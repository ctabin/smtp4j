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

import ch.astorm.smtp4j.protocol.SmtpProtocolConstants;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SmtpShutdownTest {
    private static SmtpServer smtpServer;

    @Test
    public void testCloseAllClientsOnShutdown() throws Exception {
        try (SmtpServer smtpServer = new SmtpServerBuilder()
                .withHostname("dummyhost")
                .withPort(1025)
                .start();
             Socket clientSocket = new Socket();
        ) {
            clientSocket.setSoTimeout(10_000);
            clientSocket.connect(new InetSocketAddress(InetAddress.getByName("localhost"), smtpServer.getPort()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), false, StandardCharsets.US_ASCII);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.US_ASCII));

            String readResult = reader.readLine();
            assertEquals("220 dummyhost smtp4j server ready", readResult);

            smtpServer.close();

            writer.print("HELO localhost" + SmtpProtocolConstants.CRLF);
            writer.flush();

            try {
                readResult = reader.readLine();
                assertNull(readResult);
            } catch (SocketException e) {
                // e.g. a "Connection reset" can happen in this case
            }
        }
    }
}
