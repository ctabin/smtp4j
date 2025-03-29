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

import ch.astorm.smtp4j.firewall.SmtpFirewall;
import ch.astorm.smtp4j.util.MimeMessageBuilder;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SmtpFirewalledServerMessageTest {
    private static SmtpServer smtpServer;

    private static class DynamicSmtpFirewall implements SmtpFirewall {
        private boolean acceptRemote = true;
        private boolean acceptFrom = true;
        private boolean acceptRecipient = true;
        private boolean acceptMessage = true;

        @Override
        public boolean accept(InetAddress inetAddress) {
            return acceptRemote;
        }

        @Override
        public InputStream firewallInputStream(InputStream inputStream) {
            return SmtpFirewall.super.firewallInputStream(inputStream);
        }

        @Override
        public boolean isAllowedFrom(String mailFrom) {
            assertNotNull(mailFrom);

            return acceptFrom;
        }

        @Override
        public boolean isAllowedRecipient(String recipient) {
            assertNotNull(recipient);

            return acceptRecipient;
        }

        @Override
        public boolean isAllowedMessage(String message) {
            assertNotNull(message);

            return acceptMessage;
        }

        public void reset() {
            acceptRemote = true;
            acceptFrom = true;
            acceptRecipient = true;
            acceptMessage = true;
        }

        public void setAcceptRemote(boolean acceptRemote) {
            this.acceptRemote = acceptRemote;
        }

        public void setAcceptFrom(boolean acceptFrom) {
            this.acceptFrom = acceptFrom;
        }

        public void setAcceptRecipient(boolean acceptRecipient) {
            this.acceptRecipient = acceptRecipient;
        }

        public void setAcceptMessage(boolean acceptMessage) {
            this.acceptMessage = acceptMessage;
        }
    }

    private final static DynamicSmtpFirewall firewall = new DynamicSmtpFirewall();

    @BeforeAll
    public static void init() throws Exception {
        smtpServer = new SmtpServerBuilder()
                .withPort(1025)
                .withFirewall(firewall)
                .start();
    }

    @AfterAll
    public static void after() throws Exception {
        smtpServer.close();
    }

    @AfterEach
    public void reset() {
        firewall.reset();
    }

    @Test
    public void testAcceptRemote() throws Exception {
        firewall.setAcceptRemote(false);
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer)
                .from("testAcceptRemote@smtp4j.local")
                .to("target@smtp4j.local")
                .subject("Test simple message 1")
                .body("Test simple message 1");

        String message = assertThrows(MessagingException.class, messageBuilder::send).getMessage();
        assertTrue(message.contains("response: [EOF]"));
    }

    @Test
    public void testAcceptFrom() throws Exception {
        firewall.setAcceptFrom(false);
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer)
                .from("testAcceptFrom@smtp4j.local")
                .to("target@smtp4j.local")
                .subject("Test simple message 1")
                .body("Test simple message 1");

        try {
            messageBuilder.send();
            fail("Should not be able to send message");
        } catch (MessagingException e) {
            String message = e.getMessage().trim();
            assertEquals("403 Mail-From forbidden", message);
        }
    }

    @Test
    public void testAcceptRecipient() throws Exception {
        firewall.setAcceptRecipient(false);
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer)
                .from("testAcceptRecipient@smtp4j.local")
                .to("target@smtp4j.local")
                .subject("Test simple message 1")
                .body("Test simple message 1");

        try {
            messageBuilder.send();
            fail("Should not be able to send message");
        } catch (MessagingException e) {
            String message = e.getCause().getMessage().trim();
            assertEquals("403 Recipient forbidden", message);
        }
    }

    @Test
    public void testAcceptMessage() throws Exception {
        firewall.setAcceptMessage(false);
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer)
                .from("testAcceptMessage@smtp4j.local")
                .to("target@smtp4j.local")
                .subject("Test simple message 1")
                .body("Test simple message 1");

        try {
            messageBuilder.send();
            fail("Should not be able to send message");
        } catch (MessagingException e) {
            String message = e.getMessage().trim();
            assertEquals("403 Message forbidden", message);
        }
    }
}
