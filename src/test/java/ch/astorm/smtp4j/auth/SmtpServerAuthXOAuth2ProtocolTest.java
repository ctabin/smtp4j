/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ch.astorm.smtp4j.auth;

import ch.astorm.smtp4j.SmtpServer;
import ch.astorm.smtp4j.SmtpServerBuilder;
import ch.astorm.smtp4j.SmtpServerOptions.Protocol;
import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.secure.DefaultSSLContextProvider;
import ch.astorm.smtp4j.util.MimeMessageBuilder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class SmtpServerAuthXOAuth2ProtocolTest {
    @Test
    public void testXOAuth2Authentication() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try(SmtpServer smtpServer = builder.
            withAuthenticator(XOAuth2AuthenticationHandler.INSTANCE).
            withUser("service", "lwer*4glé%&!.XLçw").
            withPort(1025).
            start()) {

            {
                MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer.createAuthenticatedSession("service", "lwer*4glé%&!.XLçw")).
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
    public void testXOAuth2AuthenticationSMTPS() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try(SmtpServer smtpServer = builder.
            withAuthenticator(XOAuth2AuthenticationHandler.INSTANCE).
            withProtocol(Protocol.SMTPS).
            withSSLContextProvider(DefaultSSLContextProvider.selfSigned()).
            withUser("service", "someToken").
            withPort(1025).
            start()) {

            {
                MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer.createAuthenticatedSession("service", "someToken")).
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
}
