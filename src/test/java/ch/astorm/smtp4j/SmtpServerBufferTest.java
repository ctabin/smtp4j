
package ch.astorm.smtp4j;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.Message.RecipientType;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class SmtpServerBufferTest {
    
    @Test
    public void testBufferChange() throws Exception {
        SmtpServer smtpServer = new SmtpServerBuilder().build();
        smtpServer.setBufferSize(1024);
        assertEquals(1024, smtpServer.getBufferSize());
        
        assertThrows(IllegalArgumentException.class, () -> smtpServer.setBufferSize(0));
        assertThrows(IllegalArgumentException.class, () -> smtpServer.setBufferSize(-10));
        
        smtpServer.start();
        assertThrows(IllegalStateException.class, () -> smtpServer.setBufferSize(1024));
        smtpServer.close();
    }
    
    @Test
    public void testMessageLargeAttachementWithSmallBuffer() throws Exception {
        try(SmtpServer smtpServer = new SmtpServerBuilder().withBufferSize(1024).start()) {
            Properties smtpProps = new Properties();
            smtpProps.setProperty("mail.smtp.host", "localhost");
            smtpProps.setProperty("mail.smtp.port", ""+smtpServer.getPort());
            
            Session session = Session.getDefaultInstance(smtpProps);
            MimeMessage msg = new MimeMessage(session);

            msg.setFrom(new InternetAddress("source@smtp4j.local"));
            msg.addRecipient(RecipientType.TO, new InternetAddress("targer@smtp4j.local"));
            msg.setSubject("Message with multiple attachments", StandardCharsets.UTF_8.name());

            MimeBodyPart bodyPart = new MimeBodyPart();
            bodyPart.setText("There is your content", StandardCharsets.UTF_8.name());

            Multipart mp = new MimeMultipart();
            mp.addBodyPart(bodyPart);

            String fileContent;
            {
                StringBuilder builder = new StringBuilder(1024);
                for(int i=0 ; i<10000 ; ++i) { builder.append("This is some file content. - Enjoy !\r\n"); }
                fileContent = builder.toString();

                MimeBodyPart attachTxtPart = new MimeBodyPart();
                attachTxtPart.setDataHandler(new DataHandler(new ByteArrayDataSource(fileContent.getBytes(StandardCharsets.UTF_8), "text/plain")));
                attachTxtPart.setFileName("data.txt");
                mp.addBodyPart(attachTxtPart);
            }

            msg.setContent(mp);

            Transport.send(msg);

            //since the buffer is too low, we expect here that the data has been dropped
            //and hence the whole message has been discarded
            assertEquals(0, smtpServer.getReceivedMessages().size());

            smtpServer.clearReceivedMessages();
            assertTrue(smtpServer.getReceivedMessages().isEmpty());
        }
    }
}
