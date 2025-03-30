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

import ch.astorm.smtp4j.core.DefaultSmtpMessageHandler;
import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.protocol.SmtpProtocolConstants;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmtpServerTest {

    @Test
    public void testDynamicPortLookup() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try (SmtpServer server1 = builder.start();
             SmtpServer server2 = builder.start();
             SmtpServer server3 = builder.start()) {
            assertNotEquals(server1.getPort(), server2.getPort());
            assertNotEquals(server1.getPort(), server3.getPort());
            assertNotEquals(server2.getPort(), server3.getPort());
        }
    }

    @Test
    public void testMultipleStartClose() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try (SmtpServer server = builder.start()) {
            assertNotNull(server.getMessageHandler());
            assertThrows(IllegalStateException.class, server::start);
        }
    }

    @Test
    public void testMultipleStartClose2() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        SmtpServer server = builder.build();
        assertFalse(server.isRunning());
        assertTrue(server.isClosed());
        server.close();

        server.start();
        assertTrue(server.isRunning());
        assertFalse(server.isClosed());
        int port = server.getPort();
        server.close();

        assertTrue(server.isClosed());
        server.close();

        server.start();
        assertFalse(server.isClosed());
        assertEquals(port, server.getPort());
        server.close();

        assertTrue(server.isClosed());
    }

    @Test
    public void testProperties() throws Exception {
        SmtpServer serverWithStaticPort = new SmtpServer(1025);
        assertEquals("localhost", serverWithStaticPort.getSessionProperties().getProperty("mail.smtp.host"));
        assertEquals("1025", serverWithStaticPort.getSessionProperties().getProperty("mail.smtp.port"));

        SmtpServer serverWithDynamicPort = new SmtpServer(0);
        assertThrows(IllegalStateException.class, serverWithDynamicPort::getSessionProperties);

        serverWithDynamicPort.start();
        assertEquals("localhost", serverWithDynamicPort.getSessionProperties().getProperty("mail.smtp.host"));
        assertEquals("" + serverWithDynamicPort.getPort(), serverWithDynamicPort.getSessionProperties().getProperty("mail.smtp.port"));
        serverWithDynamicPort.close();
    }

    @Test
    public void testListeners() {
        DefaultSmtpMessageHandler store = new DefaultSmtpMessageHandler();
        try (SmtpServer server = new SmtpServer(1025)) {
            server.addListener(store);
            server.addListener(store);
            assertEquals(2, server.getListeners().size());
            assertTrue(server.removeListener(store));
            assertTrue(server.removeListener(store));
            assertFalse(server.removeListener(store));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testNoWaitDelay() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try (SmtpServer server = builder.start()) {
            List<SmtpMessage> messages = server.readReceivedMessages(-1, TimeUnit.MILLISECONDS);
            assertTrue(messages.isEmpty());
        }
    }

    @Test
    public void testMessageRefused() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try (SmtpServer server = builder.start()) {
            server.addListener((_, _) -> {
                throw new IllegalStateException("Message refused");
            });

            MessagingException me = assertThrows(MessagingException.class, () -> new MimeMessageBuilder(server)
                    .to("test@astorm.ch")
                    .subject("Test")
                    .body("Hello!")
                    .send());
            assertEquals(SmtpProtocolConstants.CODE_TRANSACTION_FAILED + " Message refused", me.getMessage().trim());
        }
    }
}
