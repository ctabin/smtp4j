
package ch.astorm.smtp4j.secure;

import ch.astorm.smtp4j.SmtpServer;
import ch.astorm.smtp4j.SmtpServerBuilder;
import ch.astorm.smtp4j.SmtpServerOptions.Protocol;
import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.util.MimeMessageBuilder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class SmtpServerSecureProtocolTest {
    @Test
    public void testSMTPSWithoutStartTLS() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try(SmtpServer smtpServer = builder.
            withStartTLSSupport(false).
            withSSLContextProvider(DefaultSSLContextProvider.selfSigned()).
            withProtocol(Protocol.SMTPS).
            withPort(1025).
            start()) {

            MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer).
                from("source@smtp4j.local").
                to("target@smtp4j.local").
                subject("Message with multiple attachments").
                body("Hello,\nThis is some content.\n\nBye.");

            messageBuilder.send();

            List<SmtpMessage> received = smtpServer.readReceivedMessages();
            assertEquals(1, received.size());
        }
    }

    @Test
    public void testSMTPSWithStartTLS() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try(SmtpServer smtpServer = builder.
            withStartTLSSupport(false).
            withSSLContextProvider(DefaultSSLContextProvider.selfSigned()).
            withProtocol(Protocol.SMTPS).
            withStartTLSSupport(true).
            withPort(1025).
            start()) {

            //actually the STARTTLS command is not allowed since the protocol is already secure
            MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer).
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
