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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SmtpLimitedServerMessageTest {
    private static SmtpServer smtpServer;

    @BeforeAll
    public static void init() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        smtpServer = builder.withPort(1025)
                .withMaxMessageSize(1024)
                .start();
    }

    @AfterAll
    public static void after() throws Exception {
        smtpServer.close();
    }

    @Test
    public void testSimpleMessage() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer)
                .from("source@smtp4j.local")
                .to("target@smtp4j.local")
                .subject("Test simple message 1")
                .body("Test simple message 1");

        messageBuilder.send();

        List<SmtpMessage> messages = smtpServer.readReceivedMessages();
        assertEquals(1, messages.size());
        assertEquals("Test simple message 1", messages.getFirst().getSubject());


        messageBuilder = new MimeMessageBuilder(smtpServer)
                .from("source@smtp4j.local")
                .to("target@smtp4j.local")
                .subject("Test simple message 2")
                .body("Test simple message 2");

        messageBuilder.send();

        messages = smtpServer.readReceivedMessages();
        assertEquals(1, messages.size());
        assertEquals("Test simple message 2", messages.getFirst().getSubject());
    }

    @Test
    public void testMessageLargeAttachment() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer)
                .from("source@smtp4j.local")
                .to("target@smtp4j.local")
                .subject("Message with multiple attachments")
                .body("Message with multiple attachments");

        String fileContent;
        {
            fileContent = "This is some file content. - Enjoy !\r\n".repeat(10000);

            messageBuilder.attachment("data.txt", "text/plain", new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8)));
        }

        assertThrows(MessagingException.class, messageBuilder::send);
    }
}
