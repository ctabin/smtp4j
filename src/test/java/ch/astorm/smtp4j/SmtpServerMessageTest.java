
package ch.astorm.smtp4j;

import ch.astorm.smtp4j.core.SmtpAttachment;
import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.protocol.SmtpExchange;
import ch.astorm.smtp4j.secure.DefaultSSLContextProvider;
import ch.astorm.smtp4j.util.MimeMessageBuilder;
import jakarta.activation.DataHandler;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SmtpServerMessageTest {
    private static SmtpServer smtpServer;

    @BeforeAll
    public static void init() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        smtpServer = builder.
            withConnectionString("custom connection string").
            withEHLOResponseFunction(r -> "welcome!").
            withStartTLSSupport(false).
            withSSLContextProvider(DefaultSSLContextProvider.selfSigned()).
            withDebugStream(System.err).
            withPort(1025).
            start();
    }

    @AfterAll
    public static void after() throws Exception {
        smtpServer.close();
    }

    @Test
    public void testSimpleMessageUTF8() throws Exception {
        Session session = smtpServer.createSession();
        MimeMessage msg = new MimeMessage(session);

        msg.setFrom(new InternetAddress("from@local.host"));
        msg.addRecipient(RecipientType.TO, new InternetAddress("target1@local.host"));
        msg.addRecipient(RecipientType.TO, new InternetAddress("target2@local.host"));
        msg.addRecipient(RecipientType.CC, new InternetAddress("target3@local.host"));
        msg.addRecipient(RecipientType.BCC, new InternetAddress("target4@local.host"));
        msg.setSubject("Subject éèôîï & Â%=", StandardCharsets.UTF_8.name());
        msg.setText("Hello,\r\nThis is sôme CON=TENT with Spe$ial Ch@rs\r\nBye.", StandardCharsets.UTF_8.name());

        Transport.send(msg);

        List<SmtpMessage> received = smtpServer.readReceivedMessages();
        assertEquals(1, received.size());

        SmtpMessage message = received.get(0);
        assertEquals("from@local.host", message.getFrom());
        assertEquals("from@local.host", message.getSourceFrom());
        assertEquals(List.of("target1@local.host","target2@local.host","target3@local.host","target4@local.host"), message.getSourceRecipients());
        assertEquals(List.of("target1@local.host","target2@local.host"), message.getRecipients(RecipientType.TO));
        assertEquals(List.of("target3@local.host"), message.getRecipients(RecipientType.CC));
        assertTrue(message.getRecipients(RecipientType.BCC).isEmpty());
        assertEquals("Subject éèôîï & Â%=", message.getSubject());
        assertEquals("Hello,\r\nThis is sôme CON=TENT with Spe$ial Ch@rs\r\nBye.", message.getBody());
        assertTrue(message.getAttachments().isEmpty());
        assertNotNull(message.getMimeMessage());
        assertNotNull(message.getRawMimeContent());
        
        for(SmtpExchange e : message.getSmtpExchanges()) {
            assertNotNull(e.getReceivedData());
            assertNotNull(e.getRepliedData());
        }
    }
    
    @Test
    public void testSimpleMessageISO88591() throws Exception {
        Session session = smtpServer.createSession();
        MimeMessage msg = new MimeMessage(session);

        msg.setFrom(new InternetAddress("from@local.host"));
        msg.addRecipient(RecipientType.TO, new InternetAddress("target1@local.host"));
        msg.addRecipient(RecipientType.TO, new InternetAddress("target2@local.host"));
        msg.addRecipient(RecipientType.CC, new InternetAddress("target3@local.host"));
        msg.addRecipient(RecipientType.BCC, new InternetAddress("target4@local.host"));
        msg.setSubject("Subject éèôîï & Â%=", StandardCharsets.ISO_8859_1.name());
        msg.setText("Hello,\r\nThis is sôme CON=TENT with Spe$ial Ch@rs\r\nBye.", StandardCharsets.ISO_8859_1.name());

        Transport.send(msg);

        List<SmtpMessage> received = smtpServer.readReceivedMessages();
        assertEquals(1, received.size());

        SmtpMessage message = received.get(0);
        assertEquals("from@local.host", message.getFrom());
        assertEquals("from@local.host", message.getSourceFrom());
        assertEquals(List.of("target1@local.host","target2@local.host","target3@local.host","target4@local.host"), message.getSourceRecipients());
        assertEquals(List.of("target1@local.host","target2@local.host"), message.getRecipients(RecipientType.TO));
        assertEquals(List.of("target3@local.host"), message.getRecipients(RecipientType.CC));
        assertTrue(message.getRecipients(RecipientType.BCC).isEmpty());
        assertEquals("Subject éèôîï & Â%=", message.getSubject());
        assertEquals("Hello,\r\nThis is sôme CON=TENT with Spe$ial Ch@rs\r\nBye.", message.getBody());
        assertTrue(message.getAttachments().isEmpty());
        assertNotNull(message.getMimeMessage());
        assertNotNull(message.getRawMimeContent());
        
        for(SmtpExchange e : message.getSmtpExchanges()) {
            assertNotNull(e.getReceivedData());
            assertNotNull(e.getRepliedData());
        }
    }

    @Test
    public void testMultipleAddresses() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer);
        messageBuilder.from("from@local.host").
            to("target1to@local.host, Cédric 2 TO <target2@smtp4j.local>", "another-targer@smtp4j.local").
            cc("target1cc@local.host, Rôgë 2 CC <target2@smtp4j.local>").
            bcc("target1bcc@local.host, Îrfen 2 BCC <target2@smtp4j.local>").
            toRecipient(RecipientType.TO, "anotherTo@smtp4j.local").
            subject("Subject test").
            body("Some simple message");
        messageBuilder.send();

        List<SmtpMessage> received = smtpServer.readReceivedMessages();
        assertEquals(1, received.size());

        SmtpMessage message = received.get(0);
        assertEquals(List.of("target1to@local.host", "Cédric 2 TO <target2@smtp4j.local>","another-targer@smtp4j.local","anotherTo@smtp4j.local"), message.getRecipients(RecipientType.TO));
        assertEquals(List.of("target1cc@local.host", "Rôgë 2 CC <target2@smtp4j.local>"), message.getRecipients(RecipientType.CC));
    }
    
    @Test
    public void testSimpleMessageMultiPart() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer);
        messageBuilder.from(new InternetAddress("from@local.host")).
                       to(new InternetAddress("target1@local.host")).
                       to(new InternetAddress("target2@local.host")).
                       cc(new InternetAddress("target3@local.host")).
                       bcc(new InternetAddress("target4@local.host")).
                       subject("Subject éèôîï & Â%=", StandardCharsets.UTF_8).
                       body("Hello,\r\nThis is sôme CON=TENT with Spe$ial Ch@rs\r\nBye.", StandardCharsets.UTF_8);
        messageBuilder.send();
        
        List<SmtpMessage> received = smtpServer.readReceivedMessages();
        assertEquals(1, received.size());

        SmtpMessage message = received.get(0);
        assertEquals("from@local.host", message.getFrom());
        assertEquals("from@local.host", message.getSourceFrom());
        assertEquals(List.of("target1@local.host","target2@local.host","target3@local.host","target4@local.host"), message.getSourceRecipients());
        assertEquals(List.of("target1@local.host","target2@local.host"), message.getRecipients(RecipientType.TO));
        assertEquals(List.of("target3@local.host"), message.getRecipients(RecipientType.CC));
        assertTrue(message.getRecipients(RecipientType.BCC).isEmpty());
        assertEquals("Subject éèôîï & Â%=", message.getSubject());
        assertEquals("Hello,\r\nThis is sôme CON=TENT with Spe$ial Ch@rs\r\nBye.", message.getBody());
        assertTrue(message.getAttachments().isEmpty());
        assertNotNull(message.getMimeMessage());
        assertNotNull(message.getRawMimeContent());
    }
    
    @Test
    public void testSimpleMessageMultiPartNoCharset() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer);
        messageBuilder.from("from@local.host").
            to("target1@local.host").
            to("target2@local.host").
            cc("target3@local.host").
            bcc("target4@local.host").
            at("31.12.2020 23:59:59").
            subject("Subject éèôîï & Â%=").
            body("Hello,\r\nThis is sôme CON=TENT with Spe$ial Ch@rs\r\nBye.");
        messageBuilder.send();
        
        assertThrows(IllegalStateException.class, () -> messageBuilder.send());
        
        List<SmtpMessage> received = smtpServer.readReceivedMessages();
        assertEquals(1, received.size());

        SmtpMessage message = received.get(0);
        assertEquals("from@local.host", message.getFrom());
        assertEquals("from@local.host", message.getSourceFrom());
        assertEquals(List.of("target1@local.host","target2@local.host","target3@local.host","target4@local.host"), message.getSourceRecipients());
        assertEquals(List.of("target1@local.host","target2@local.host"), message.getRecipients(RecipientType.TO));
        assertEquals(List.of("target3@local.host"), message.getRecipients(RecipientType.CC));
        assertTrue(message.getRecipients(RecipientType.BCC).isEmpty());
        assertEquals(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").parse("31.12.2020 23:59:59"), message.getSentDate());
        assertEquals("Subject éèôîï & Â%=", message.getSubject());
        assertEquals("Hello,\r\nThis is sôme CON=TENT with Spe$ial Ch@rs\r\nBye.", message.getBody());
        assertTrue(message.getAttachments().isEmpty());
        assertNotNull(message.getMimeMessage());
        assertNotNull(message.getRawMimeContent());
    }

    @Test
    public void testMessageWithAttachment() throws Exception {
        Session session = smtpServer.createSession();
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

        List<SmtpMessage> received = smtpServer.readReceivedMessages();
        assertEquals(1, received.size());
        assertTrue(smtpServer.readReceivedMessages().isEmpty());
        
        SmtpMessage message = received.get(0);
        assertEquals("Cédric <info@smtp4j.local>", message.getFrom());
        assertEquals("info@smtp4j.local", message.getSourceFrom());
        assertEquals(List.of("Méphisto <user_daemon@underground.local>"), message.getRecipients(RecipientType.TO));
        assertEquals(List.of("user_daemon@underground.local"), message.getSourceRecipients());
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
    }

    @Test
    public void testMessageWithMultipleAttachments() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer);
        messageBuilder.from("source@smtp4j.local").
            to("target@smtp4j.local").
            subject("Message with multiple attachments", StandardCharsets.UTF_8).
            body("There is your content", StandardCharsets.UTF_8);
        
        String dynContent = "";
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try(ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("content/"));
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("content/file.txt"));
                for(int i=0 ; i<5000 ; ++i) {
                    String toAdd = "some TXT Content. #/";;
                    if(i%100==0 && i>0) { toAdd += "\r\n"; }

                    dynContent += toAdd;

                    byte[] strBytes = toAdd.getBytes(StandardCharsets.UTF_8);
                    zos.write(strBytes);
                }
                zos.closeEntry();
            }

            messageBuilder.attachment("file.zip", "application/octed-stream", new ByteArrayInputStream(baos.toByteArray()));
        }

        String fileContent;
        {
            StringBuilder builder = new StringBuilder(1024);
            for(int i=0 ; i<50000 ; ++i) { builder.append("This is some file content. - Enjoy !\r\n"); }
            fileContent = builder.toString();
            
            messageBuilder.attachment("data.txt", "text/plain", new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8)));
        }

        MimeMessage mimeMessage = messageBuilder.send();
        assertNotNull(mimeMessage);

        List<SmtpMessage> received = smtpServer.readReceivedMessages();
        assertEquals(1, received.size());

        SmtpMessage message = received.get(0);
        List<SmtpAttachment> attachments = message.getAttachments();
        assertEquals(2, attachments.size());

        {
            SmtpAttachment attachment = attachments.get(0);
            assertEquals("file.zip", attachment.getFilename());

            try(ZipInputStream zis = new ZipInputStream(attachment.openStream())) {
                assertEquals("content/", zis.getNextEntry().getName());
                zis.closeEntry();

                ZipEntry fileEntry = zis.getNextEntry();
                assertEquals("content/file.txt", fileEntry.getName());

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int read = zis.read(buffer);
                while(read>=0) {
                    baos.write(buffer, 0, read);
                    read = zis.read(buffer);
                }

                String str = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                assertEquals(dynContent, str);
            }
        }

        {
            SmtpAttachment attachment = attachments.get(1);
            assertEquals("data.txt", attachment.getFilename());

            StringBuilder builder = new StringBuilder();
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(attachment.openStream()))) {
                String str = reader.readLine();
                while(str!=null) {
                    builder.append(str).append("\r\n");
                    str = reader.readLine();
                }
            }
            assertEquals(fileContent, builder.toString());
        }
    }
    
    @Test
    public void testMessageLargeAttachement() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer).
                from("source@smtp4j.local").
                to("target@smtp4j.local").
                subject("Message with multiple attachments").
                body("Message with multiple attachments");

        String fileContent;
        {
            StringBuilder builder = new StringBuilder(1024);
            for(int i=0 ; i<10000 ; ++i) { builder.append("This is some file content. - Enjoy !\r\n"); }
            fileContent = builder.toString();

            messageBuilder.attachment("data.txt", "text/plain", new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8)));
        }

        messageBuilder.send();

        //since the buffer is too low, we expect here that the data has been dropped
        //and hence the whole message has been discarded
        assertEquals(1, smtpServer.readReceivedMessages().size());
    }
    
    @Test
    public void testMessageMultipartWithoutBody() throws Exception {
        Session session = smtpServer.createSession();
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

        List<SmtpMessage> received = smtpServer.readReceivedMessages();
        assertEquals(1, received.size());

        SmtpMessage message = received.get(0);

        assertEquals("info@smtp4j.local", message.getFrom());
        assertEquals(List.of("target@smtp4j.local"), message.getRecipients(RecipientType.TO));
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
    }
    
    @Test
    public void testMessageMultipartWithMultipleBody() throws Exception {
        Session session = smtpServer.createSession();
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

        List<SmtpMessage> received = smtpServer.readReceivedMessages();
        assertEquals(1, received.size());

        SmtpMessage message = received.get(0);

        assertEquals("info@smtp4j.local", message.getFrom());
        assertEquals(List.of("target@smtp4j.local"), message.getRecipients(RecipientType.TO));
        assertEquals("Hi buddy", message.getSubject());
        assertEquals("Content BODY1\r\nContent BODY2", message.getBody());

        List<SmtpAttachment> attachments = message.getAttachments();
        assertEquals(0, attachments.size());
    }
    
    @Test
    public void testSpecialMessageDataForDot() throws Exception {
        String text = "This méssage has multiple lines in SMTP and a dot.dot.dot.dot.dot.dot..dot.";
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer).
            from("source@smtp4j.local").
            to("target@smtp4j.local").
            subject("Message with multiple attachments").
            body(text);

        messageBuilder.send();

        List<SmtpMessage> received = smtpServer.readReceivedMessages();
        assertEquals(1, received.size());

        SmtpMessage message = received.get(0);
        String body = message.getBody();
        assertEquals(text, body);
    }
    
    @Test
    public void testDisabledStartTLS() throws Exception {
        Properties props = smtpServer.getSessionProperties();
        props.put("mail.smtp.starttls.enable", "true");
        
        Session session = Session.getInstance(props);
        
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(session).
            from("source@smtp4j.local").
            to("target@smtp4j.local").
            subject("Subject").
            body("Message");

        messageBuilder.send();
        assertEquals(1, smtpServer.readReceivedMessages().size());
    }
    
    @Test
    public void testEnabledStartTLS() throws Exception {
        smtpServer.getOptions().starttls = true;
        try {
            MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer).
                from("source@smtp4j.local").
                to("target@smtp4j.local").
                subject("Subject").
                body("Message");

            messageBuilder.send();
            assertEquals(1, smtpServer.readReceivedMessages().size());
        } finally {
            smtpServer.getOptions().starttls = false;
        }
    }
}
