
package ch.astorm.smtp4j.core;

import ch.astorm.smtp4j.protocol.SmtpCommand.Type;
import ch.astorm.smtp4j.protocol.SmtpProtocolConstants;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

/**
 * Represents an SMTP message.
 */
public class SmtpMessage {
    private String sourceFrom;
    private List<String> sourceRecipients;
    private MimeMessage mimeMessage;
    private String rawMimeContent;

    /**
     * Creates a new {@code SmtpMessage} with the specified parameters.
     *
     * @param from The source {@code From} parameter value.
     * @param recipients The source {@code Rcpt} parameter values.
     * @param mimeMessage The parsed {@code MimeMessage}.
     * @param rawMimeContent The raw MIME content of {@code mimeMessage}.
     */
    public SmtpMessage(String from, List<String> recipients, MimeMessage mimeMessage, String rawMimeContent) {
        this.sourceFrom = from;
        this.sourceRecipients = recipients;
        this.mimeMessage = mimeMessage;
        this.rawMimeContent = rawMimeContent;
    }

    /**
     * Returns the {@code From} parameter specified during the protocol exchange.
     * This value will contain only the email (info@mydomain.com).
     *
     * @return The {@code MAIL FROM:} value.
     * @see Type#MAIL_FROM
     */
    public String getSourceFrom() {
        return sourceFrom;
    }

    /**
     * Returns the list of {@code To} parameters specified during the protocol exchange.
     * The values will contain only the email (info@mydomain.com).
     * <p>Note that all recipients (including {@link RecipientType#BCC}) will be present in this list.</p>
     *
     * @return The {@code RCPT TO:} values.
     * @see Type#RECIPIENT
     */
    public List<String> getSourceRecipients() {
        return sourceRecipients;
    }
    
    /**
     * Returns the {@code From} header of the MIME message.
     * This value can be composed, for instance: {@code Cédric <info@mydomain.com>}.
     * 
     * @return The {@code From} header.
     */
    public String getFrom() {
        try {
            Address[] fromAddrs = mimeMessage.getFrom();
            if(fromAddrs==null || fromAddrs.length==0) { return null; }
            return MimeUtility.decodeText(mimeMessage.getFrom()[0].toString());
        } catch(UnsupportedEncodingException | MessagingException e) {
            throw new RuntimeException("Unable to retrieve From header", e);
        }
    }

    /**
     * Returns all the recipients of the given {@code type}.
     * Those values can be composed, for instance: {@code Cédric <info@mydomain.com>}.
     * <p>The {@link RecipientType#BCC} will always yield an empty list.</p>
     * 
     * @param type The type.
     * @return A list of recipients or an empty list if there is none.
     */
    public List<String> getRecipients(RecipientType type) {
        try {
            Address[] addrs = mimeMessage.getRecipients(type);
            if(addrs==null || addrs.length==0) { return Collections.EMPTY_LIST; }

            List<String> addressStrs = new ArrayList<>(addrs.length);
            for(Address addr : addrs) { addressStrs.add(MimeUtility.decodeText(addr.toString())); }
            return addressStrs;
        } catch(UnsupportedEncodingException | MessagingException e) {
            throw new RuntimeException("Unable to retrieve Recipients "+type, e);
        }
    }

    /**
     * Returns the {@code Subject} header of the MIME message.
     * 
     * @return The {@code Subject} header.
     */
    public String getSubject() {
        try { return mimeMessage.getSubject(); }
        catch(MessagingException me) { throw new RuntimeException("Unable to retrieve Subject header", me); }
    }

    /**
     * Returns the content of the MIME message.
     * If the underlying {@code MimeMessage} is a {@code MimeMultipart}, then all the
     * parts without a filename will be concatenated together (separated by {@link SmtpProtocolConstants#CRLF}
     * and returned as the body. If there is none, then null will be returned.
     * 
     * @return The content or null.
     */
    public String getBody() {
        try {
            Object content = mimeMessage.getContent();
            if(content==null) { return null; }

            if(content instanceof MimeMultipart) {
                MimeMultipart multipart = (MimeMultipart)content;
                if(multipart.getCount()==0) { throw new IllegalStateException("At least one part expected"); }

                StringBuilder builder = new StringBuilder();
                for(int i=0 ; i<multipart.getCount() ; ++i) {
                    BodyPart body = multipart.getBodyPart(i);
                    if(body.getFileName()==null) {
                        if(builder.length()>0) { builder.append(SmtpProtocolConstants.CRLF); }
                        builder.append(body.getContent().toString());
                    }
                }
                
                return builder.length()>0 ? builder.toString() : null;
            } else {
                return content.toString();
            }
        } catch(IOException | MessagingException e) {
            throw new RuntimeException("Unable to retrieve content", e);
        }
    }

    /**
     * Returns the attachments of the MIME message.
     * If the underlying {@code MimeMessage} is not {@code MimeMultipart} an empty
     * list will be returned.
     * <p>Note that only parts with a name will be considered as attachment.</p>
     *
     * @return A list of attachments.
     */
    public List<SmtpAttachment> getAttachments() {
        try {
            Object content = mimeMessage.getContent();
            if(content==null) { return null; }

            if(content instanceof MimeMultipart) {
                MimeMultipart multipart = (MimeMultipart)content;
                int nbParts = multipart.getCount();

                if(nbParts==0) { throw new IllegalStateException("At least one part expected"); }

                List<SmtpAttachment> attachments = new ArrayList<>(nbParts);
                for(int i=0 ; i<nbParts ; ++i) {
                    BodyPart part = multipart.getBodyPart(i);
                    String filename = part.getFileName();
                    if(filename!=null) {
                        SmtpAttachment att = new SmtpAttachment(filename, part.getContentType(), () -> part.getInputStream());
                        attachments.add(att);
                    }
                }
                return attachments;
            } else {
                return Collections.EMPTY_LIST;
            }
        } catch(IOException | MessagingException e) {
            throw new RuntimeException("Unable to retrieve content", e);
        }
    }

    /**
     * Returns the sent date.
     *
     * @return The sent date.
     */
    public Date getSentDate() {
        try { return mimeMessage.getSentDate(); }
        catch(MessagingException e) { throw new RuntimeException("Unable to retrieve Sent date", e); }
    }
    
    /**
     * Returns the {@code MimeMessage} parsed from the content.
     * 
     * @return the {@code MimeMessage}.
     */
    public MimeMessage getMimeMessage() {
        return mimeMessage;
    }
    
    /**
     * Returns the internal raw content received by the SMTP server to parse as {@code MimeMessage}.
     * 
     * @return The raw content.
     */
    public String getRawMimeContent() {
        return rawMimeContent;
    }

    /**
     * Creates a new {@code SmtpMessage} with the specified parameters.
     *
     * @param from The source {@code From} parameter value.
     * @param recipients The source {@code Rcpt} parameter values.
     * @param mimeMessageStr The {@code MimeMessage} content.
     * @return A new {@code SmtpMessage} instance.
     */
    public static SmtpMessage create(String from, List<String> recipients, String mimeMessageStr) {
        MimeMessage mimeMessage;
        try(InputStream is = new ByteArrayInputStream(mimeMessageStr.getBytes(StandardCharsets.UTF_8))) { mimeMessage = new MimeMessage(null, is); }
        catch(IOException | MessagingException e) { throw new RuntimeException("Unable to create MimeMessage from content", e); }
        return new SmtpMessage(from, recipients, mimeMessage, mimeMessageStr);
    }
}
