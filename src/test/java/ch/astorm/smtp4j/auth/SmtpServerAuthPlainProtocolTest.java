
package ch.astorm.smtp4j.auth;

import ch.astorm.smtp4j.SmtpServer;
import ch.astorm.smtp4j.SmtpServerBuilder;
import ch.astorm.smtp4j.SmtpServerOptions;
import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.secure.DefaultSSLContextProvider;
import ch.astorm.smtp4j.util.MimeMessageBuilder;
import jakarta.mail.AuthenticationFailedException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class SmtpServerAuthPlainProtocolTest {
    @Test
    public void testPlainAuthentication() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try(SmtpServer smtpServer = builder.
            withAuthenticator(PlainAuthenticationHandler.INSTANCE).
            withUser("jdoe", "bE5HxNrCL7,:3=yP@vta6n").
            withUser("cédric", "ENrôlëmEtéà").
            withDebugStream(System.err).
            withPort(1025).
            start()) {

            {
                MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer.createAuthenticatedSession("jdoe", "bE5HxNrCL7,:3=yP@vta6n")).
                    from("source@smtp4j.local").
                    to("target@smtp4j.local").
                    subject("Message with multiple attachments").
                    body("Hello,\nThis is some content.\n\nBye.");

                messageBuilder.send();

                List<SmtpMessage> received = smtpServer.readReceivedMessages();
                assertEquals(1, received.size());
            }

            {
                MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer.createAuthenticatedSession("cédric", "ENrôlëmEtéà")).
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

    @Test
    public void testPlainAuthenticationWithStartssl() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try(SmtpServer smtpServer = builder.
            withAuthenticator(PlainAuthenticationHandler.INSTANCE).
            withUser("jdoe", "bE5HxNrCL7,:3=yP@vta6n").
            withSSLContextProvider(DefaultSSLContextProvider.selfSigned()).
            withStartTLSSupport(true).
            withPort(1025).
            start()) {

            MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer.createAuthenticatedSession("jdoe", "bE5HxNrCL7,:3=yP@vta6n")).
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
    public void testPlainAuthenticationWithSMTPS() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try(SmtpServer smtpServer = builder.
            withAuthenticator(PlainAuthenticationHandler.INSTANCE).
            withUser("jdoe", "bE5HxNrCL7,:3=yP@vta6n").
            withSSLContextProvider(DefaultSSLContextProvider.selfSigned()).
            withProtocol(SmtpServerOptions.Protocol.SMTPS).
            withPort(1025).
            start()) {

            MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer.createAuthenticatedSession("jdoe", "bE5HxNrCL7,:3=yP@vta6n")).
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
    public void testPlainAuthenticationFailure() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try(SmtpServer smtpServer = builder.
            withAuthenticator(PlainAuthenticationHandler.INSTANCE).
            withUser("jdoe", "bE5HxNrCL7,:3=yP@vta6n").
            withPort(1025).
            start()) {

            MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer.createAuthenticatedSession("jdoe", "wrongPassword")).
                from("source@smtp4j.local").
                to("target@smtp4j.local").
                subject("Message with multiple attachments").
                body("Hello,\nThis is some content.\n\nBye.");

            assertThrows(AuthenticationFailedException.class, () -> messageBuilder.send());

            MimeMessageBuilder messageBuilder2 = new MimeMessageBuilder(smtpServer.createAuthenticatedSession("wrongUser", "wrongPassword")).
                from("source@smtp4j.local").
                to("target@smtp4j.local").
                subject("Message with multiple attachments").
                body("Hello,\nThis is some content.\n\nBye.");

            assertThrows(AuthenticationFailedException.class, () -> messageBuilder2.send());
        }
    }
}
