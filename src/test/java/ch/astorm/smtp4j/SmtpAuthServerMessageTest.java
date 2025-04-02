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

import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.util.MimeMessageBuilder;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SmtpAuthServerMessageTest {
    private static SmtpServer smtpServer;

    @BeforeAll
    public static void init() throws Exception {
        smtpServer = new SmtpServerBuilder()
                .withPort(1025)
                .withAuth(user -> (user + "-password").getBytes(StandardCharsets.UTF_8))
                .start();
    }

    @AfterAll
    public static void after() throws Exception {
        smtpServer.close();
    }

    @Test
    public void testNoAuth() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer)
                .from("testNoAuth@smtp4j.local")
                .to("target@smtp4j.local")
                .subject("Test simple message 1")
                .body("Test simple message 1");

        String message = assertThrows(MessagingException.class, messageBuilder::send).getMessage().trim();
        assertEquals("550 Authentication required", message);
    }


    @Test
    public void testWrongAuth() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer)
                .from("testWrongAuth@smtp4j.local")
                .to("target@smtp4j.local")
                .subject("Test simple message 1")
                .body("Test simple message 1");

        String message = assertThrows(MessagingException.class, () -> messageBuilder.send("test", "wrong-password")).getMessage().trim();
        assertEquals("535 Authentication failed", message);
    }

    @Test
    public void testCorrectAuth() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer)
                .from("testCorrectAuth@smtp4j.local")
                .to("target@smtp4j.local")
                .subject("Test simple message 1")
                .body("Test simple message 1");

        messageBuilder.send("test", "test-password");

        List<SmtpMessage> received = smtpServer.readReceivedMessages();
        assertEquals(1, received.size());
    }
}
