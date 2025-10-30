
package ch.astorm.smtp4j.auth;

import ch.astorm.smtp4j.SmtpServer;
import ch.astorm.smtp4j.SmtpServerBuilder;
import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.util.MimeMessageBuilder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class SmtpServerAuthMultipleSchemesProtocolTest {
    @Test
    public void testMultipleAuthSchemes() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try(SmtpServer smtpServer = builder.
            withAuthentication().
            withUser("jdoe", "jpasswd").
            withDebugStream(System.err).
            withPort(1025).
            start()) {

            MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer.createAuthenticatedSession("jdoe", "jpasswd")).
                from("source@smtp4j.local").
                to("target@smtp4j.local").
                subject("Message with multiple attachments").
                body("Hello,\nThis is some content.\n\nBye.");

            messageBuilder.send();

            List<SmtpMessage> received = smtpServer.readReceivedMessages();
            assertEquals(1, received.size());
        }
    }
}
