
package ch.astorm.smtp4j;

import ch.astorm.smtp4j.core.SmtpAttachment;
import ch.astorm.smtp4j.core.SmtpMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SmtpServerMessageTest {
    private static SmtpServer smtpServer;
    private static Properties smtpProps;

    @BeforeAll
    public static void init() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        smtpServer = builder.withPort(1025).start();

        smtpProps = new Properties();
        smtpProps.setProperty("mail.smtp.host", "localhost");
        smtpProps.setProperty("mail.smtp.port", "1025");
    }

    @AfterAll
    public static void after() throws Exception {
        smtpServer.close();
    }

    @Test
    public void testSimpleMessage() throws Exception {
        Session session = Session.getDefaultInstance(smtpProps);
        MimeMessage msg = new MimeMessage(session);

        msg.setFrom(new InternetAddress("from@local.host"));
        msg.addRecipient(RecipientType.TO, new InternetAddress("target1@local.host"));
        msg.addRecipient(RecipientType.TO, new InternetAddress("target2@local.host"));
        msg.addRecipient(RecipientType.CC, new InternetAddress("target3@local.host"));
        msg.addRecipient(RecipientType.BCC, new InternetAddress("target4@local.host"));
        msg.setSubject("Subject éèôîï & Â%=", StandardCharsets.UTF_8.name());
        msg.setText("Hello,\r\nThis is sôme CON=TENT with Spe$ial Ch@rs\r\nBye.", StandardCharsets.UTF_8.name());

        Transport.send(msg);

        assertEquals(1, smtpServer.getReceivedMessages().size());

        SmtpMessage message = smtpServer.getReceivedMessages().get(0);
        assertEquals("from@local.host", message.getFrom());
        assertEquals("from@local.host", message.getSourceFrom());
        assertEquals(Arrays.asList("target1@local.host","target2@local.host","target3@local.host","target4@local.host"), message.getSourceRecipients());
        assertEquals(Arrays.asList("target1@local.host","target2@local.host"), message.getRecipients(RecipientType.TO));
        assertEquals(Arrays.asList("target3@local.host"), message.getRecipients(RecipientType.CC));
        assertTrue(message.getRecipients(RecipientType.BCC).isEmpty());
        assertEquals("Subject éèôîï & Â%=", message.getSubject());
        assertEquals("Hello,\r\nThis is sôme CON=TENT with Spe$ial Ch@rs\r\nBye.", message.getBody());
        assertTrue(message.getAttachments().isEmpty());
        assertNotNull(message.getMimeMessage());
        assertNotNull(message.getRawMimeContent());

        smtpServer.clearReceivedMessages();
        assertTrue(smtpServer.getReceivedMessages().isEmpty());
    }

    @Test
    public void testMessageWithAttachement() throws Exception {
        Session session = Session.getDefaultInstance(smtpProps);
        MimeMessage msg = new MimeMessage(session);

        Date sentDate = new SimpleDateFormat("dd.MM.yyyy HH:mm").parse("31.12.2020 23:59");
        msg.setFrom(new InternetAddress("info@smtp4j.local", "Cédric", StandardCharsets.UTF_8.name()));
        msg.addRecipient(RecipientType.TO, new InternetAddress("user_daemon@underground.local", "Méphisto", StandardCharsets.UTF_8.name()));
        msg.setSubject("Here is your _list_ of *SOULS*", StandardCharsets.UTF_8.name());
        msg.setSentDate(sentDate);

        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setText("Hi there,\nI summon you with a pretty neat spell ! Check my attachment and respond.\nDamn.\nHell.", StandardCharsets.UTF_8.name());

        Multipart mp = new MimeMultipart();
        mp.addBodyPart(bodyPart);

        MimeBodyPart attachPart = new MimeBodyPart();
        attachPart.setDataHandler(new DataHandler(new ByteArrayDataSource("Some content", "text/plain")));
        attachPart.setFileName("file.txt");
        mp.addBodyPart(attachPart);

        msg.setContent(mp);

        Transport.send(msg);

        assertEquals(1, smtpServer.getReceivedMessages().size());

        SmtpMessage message = smtpServer.getReceivedMessages().get(0);

        assertEquals("Cédric <info@smtp4j.local>", message.getFrom());
        assertEquals("info@smtp4j.local", message.getSourceFrom());
        assertEquals(Arrays.asList("Méphisto <user_daemon@underground.local>"), message.getRecipients(RecipientType.TO));
        assertEquals(Arrays.asList("user_daemon@underground.local"), message.getSourceRecipients());
        assertTrue(message.getRecipients(RecipientType.CC).isEmpty());
        assertEquals("Here is your _list_ of *SOULS*", message.getSubject());
        assertEquals(sentDate, message.getSentDate());
        assertEquals("Hi there,\r\nI summon you with a pretty neat spell ! Check my attachment and respond.\r\nDamn.\r\nHell.", message.getBody());

        List<SmtpAttachment> attachments = message.getAttachments();
        assertEquals(1, attachments.size());

        SmtpAttachment attachment = attachments.get(0);
        assertEquals("file.txt", attachment.getFilename());
        assertEquals("text/plain; charset=us-ascii; name=file.txt", attachment.getContentType());

        String attContent;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(attachment.openStream(), StandardCharsets.UTF_8))) { attContent = reader.readLine(); }
        assertEquals("Some content", attContent);

        try { attachment.openStream(); fail("Error expected"); }
        catch(IOException ioe) { /* ok */ }

        smtpServer.clearReceivedMessages();
        assertTrue(smtpServer.getReceivedMessages().isEmpty());
    }
    
    @Test
    public void testMessageMultipartWithoutBody() throws Exception {
        Session session = Session.getDefaultInstance(smtpProps);
        MimeMessage msg = new MimeMessage(session);

        msg.setFrom(new InternetAddress("info@smtp4j.local"));
        msg.addRecipient(RecipientType.TO, new InternetAddress("target@smtp4j.local"));
        msg.setSubject("Hi buddy", StandardCharsets.UTF_8.name());

        Multipart mp = new MimeMultipart();
        MimeBodyPart attachPart = new MimeBodyPart();
        attachPart.setDataHandler(new DataHandler(new ByteArrayDataSource("Some content", "text/plain")));
        attachPart.setFileName("file.txt");
        mp.addBodyPart(attachPart);

        msg.setContent(mp);

        Transport.send(msg);

        assertEquals(1, smtpServer.getReceivedMessages().size());

        SmtpMessage message = smtpServer.getReceivedMessages().get(0);

        assertEquals("info@smtp4j.local", message.getFrom());
        assertEquals(Arrays.asList("target@smtp4j.local"), message.getRecipients(RecipientType.TO));
        assertEquals("Hi buddy", message.getSubject());
        assertNull(message.getBody());

        List<SmtpAttachment> attachments = message.getAttachments();
        assertEquals(1, attachments.size());

        SmtpAttachment attachment = attachments.get(0);
        assertEquals("file.txt", attachment.getFilename());
        assertEquals("text/plain; charset=us-ascii; name=file.txt", attachment.getContentType());

        String attContent;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(attachment.openStream(), StandardCharsets.UTF_8))) { attContent = reader.readLine(); }
        assertEquals("Some content", attContent);

        try { attachment.openStream(); fail("Error expected"); }
        catch(IOException ioe) { /* ok */ }

        smtpServer.clearReceivedMessages();
        assertTrue(smtpServer.getReceivedMessages().isEmpty());
    }
    
    @Test
    public void testMessageMultipartWithMultipleBody() throws Exception {
        Session session = Session.getDefaultInstance(smtpProps);
        MimeMessage msg = new MimeMessage(session);

        msg.setFrom(new InternetAddress("info@smtp4j.local"));
        msg.addRecipient(RecipientType.TO, new InternetAddress("target@smtp4j.local"));
        msg.setSubject("Hi buddy", StandardCharsets.UTF_8.name());

        Multipart mp = new MimeMultipart();
        
        MimeBodyPart body1 = new MimeBodyPart();
        body1.setText("Content BODY1");
        mp.addBodyPart(body1);
        
        MimeBodyPart body2 = new MimeBodyPart();
        body2.setText("Content BODY2");
        mp.addBodyPart(body2);

        msg.setContent(mp);

        Transport.send(msg);

        assertEquals(1, smtpServer.getReceivedMessages().size());

        SmtpMessage message = smtpServer.getReceivedMessages().get(0);

        assertEquals("info@smtp4j.local", message.getFrom());
        assertEquals(Arrays.asList("target@smtp4j.local"), message.getRecipients(RecipientType.TO));
        assertEquals("Hi buddy", message.getSubject());
        assertEquals("Content BODY1\r\nContent BODY2", message.getBody());

        List<SmtpAttachment> attachments = message.getAttachments();
        assertEquals(0, attachments.size());

        smtpServer.clearReceivedMessages();
        assertTrue(smtpServer.getReceivedMessages().isEmpty());
    }
}
