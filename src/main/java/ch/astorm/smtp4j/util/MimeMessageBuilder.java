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

package ch.astorm.smtp4j.util;

import ch.astorm.smtp4j.SmtpServer;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.Address;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Provides methods to easily create a {@code MimeMessage} that can be sent to an
 * SMTP server.
 */
public class MimeMessageBuilder {
    private MimeMessage message;
    private MimeBodyPart body;
    private final List<MimeBodyPart> attachments = new ArrayList<>();

    /**
     * Creates a new {@code MimeMessageBuilder} by creating a new {@code Session}
     * from the given {@code server}.
     *
     * @param server The server.
     * @see SmtpServer#createSession()
     */
    public MimeMessageBuilder(SmtpServer server) {
        this(server.createSession());
    }

    /**
     * Creates a new {@code MimeMessageBuilder} with the given {@code session}.
     *
     * @param session The session.
     */
    public MimeMessageBuilder(Session session) {
        this.message = new MimeMessage(session);
    }

    /**
     * Defines the From address.
     *
     * @param from The from address.
     * @return This builder.
     */
    public MimeMessageBuilder from(String from) throws MessagingException {
        checkState();
        message.setFrom(from);
        return this;
    }

    /**
     * Defines the From address.
     *
     * @param from The from address.
     * @return This builder.
     */
    public MimeMessageBuilder from(Address from) throws MessagingException {
        checkState();
        message.setFrom(from);
        return this;
    }

    /**
     * Defines the sent date.
     *
     * @param sentDate The sent date in format 'dd.MM.yyyy HH:mm:ss' (31.12.2020 23:59:59).
     * @return This builder.
     */
    public MimeMessageBuilder at(String sentDate) throws ParseException, MessagingException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return at(sdf.parse(sentDate));
    }

    /**
     * Defines the sent date.
     *
     * @param sentDate The sent date.
     * @return This builder.
     */
    public MimeMessageBuilder at(Date sentDate) throws MessagingException {
        checkState();
        message.setSentDate(sentDate);
        return this;
    }

    /**
     * Adds the {@code address} to the {@code TO} recipients.
     *
     * @param address The address or a comma-separated list of addresses.
     * @return This builder.
     */
    public MimeMessageBuilder to(String... address) throws MessagingException {
        for (InternetAddress addr : parseAddressList(address)) {
            to(addr);
        }
        return this;
    }

    /**
     * Adds the {@code address} to the {@code TO} recipients.
     *
     * @param address The address.
     * @return This builder.
     */
    public MimeMessageBuilder to(Address address) throws MessagingException {
        return toRecipient(RecipientType.TO, address);
    }

    /**
     * Adds the {@code address} to the {@code CC} recipients.
     *
     * @param address The address or a comma-separated list of addresses.
     * @return This builder.
     */
    public MimeMessageBuilder cc(String... address) throws MessagingException {
        for (Address addr : parseAddressList(address)) {
            cc(addr);
        }
        return this;
    }

    /**
     * Adds the {@code address} to the {@code CC} recipients.
     *
     * @param address The address.
     * @return This builder.
     */
    public MimeMessageBuilder cc(Address address) throws MessagingException {
        return toRecipient(RecipientType.CC, address);
    }

    /**
     * Adds the {@code address} to the {@code BCC} recipients.
     *
     * @param address The address or a comma-separated list of addresses.
     * @return This builder.
     */
    public MimeMessageBuilder bcc(String... address) throws MessagingException {
        for (Address addr : parseAddressList(address)) {
            bcc(addr);
        }
        return this;
    }

    /**
     * Adds the {@code address} to the {@code BCC} recipients.
     *
     * @param address The address.
     * @return This builder.
     */
    public MimeMessageBuilder bcc(Address address) throws MessagingException {
        return toRecipient(RecipientType.BCC, address);
    }

    /**
     * Adds the {@code address} to the specified recipient {@code type}.
     *
     * @param type    The recipient type.
     * @param address The address or a comma-separated list of addresses.
     * @return This builder.
     */
    public MimeMessageBuilder toRecipient(RecipientType type, String... address) throws MessagingException {
        for (Address addr : parseAddressList(address)) {
            toRecipient(type, addr);
        }
        return this;
    }

    /**
     * Adds the {@code address} to the specified recipient {@code type}.
     *
     * @param type    The recipient type.
     * @param address The address.
     * @return This builder.
     */
    public MimeMessageBuilder toRecipient(RecipientType type, Address address) throws MessagingException {
        checkState();
        message.addRecipient(type, address);
        return this;
    }

    /**
     * Defines the message subject.
     *
     * @param subject The subject.
     * @return This builder.
     */
    public MimeMessageBuilder subject(String subject) throws MessagingException {
        checkState();
        message.setSubject(subject);
        return this;
    }

    /**
     * Defines the message subject.
     *
     * @param subject The subject.
     * @param charset The character set.
     * @return This builder.
     */
    public MimeMessageBuilder subject(String subject, Charset charset) throws MessagingException {
        checkState();
        message.setSubject(subject, charset.name());
        return this;
    }

    /**
     * Defines the message body.
     *
     * @param body The body.
     * @return This builder.
     */
    public MimeMessageBuilder body(String body) throws MessagingException {
        checkState();

        this.body = new MimeBodyPart();
        this.body.setText(body);
        return this;
    }

    /**
     * Defines the message body.
     *
     * @param body    The body.
     * @param charset The character set.
     * @return This builder.
     */
    public MimeMessageBuilder body(String body, Charset charset) throws MessagingException {
        checkState();

        this.body = new MimeBodyPart();
        this.body.setText(body, charset.name());
        return this;
    }

    /**
     * Adds the specified {@code file} as attachment.
     *
     * @param file The file.
     * @return This builder.
     */
    public MimeMessageBuilder attachment(File file) throws MessagingException {
        return attachment(file.getName(), file);
    }

    /**
     * Adds the specified {@code file} as attachment wit the given {@code name}.
     *
     * @param name The name.
     * @param file The file.
     * @return This builder.
     */
    public MimeMessageBuilder attachment(String name, File file) throws MessagingException {
        checkState();

        MimeBodyPart attachPart = new MimeBodyPart();
        attachPart.setDataHandler(new DataHandler(new FileDataSource(file)));
        attachPart.setFileName(name);
        attachments.add(attachPart);
        return this;
    }

    /**
     * Adds the specified {@code file} as attachment wit the given {@code name} and {@code mimeType}.
     *
     * @param name     The name.
     * @param mimeType The MIME type.
     * @param is       The input stream.
     * @return This builder.
     */
    public MimeMessageBuilder attachment(String name, String mimeType, InputStream is) throws IOException, MessagingException {
        checkState();

        MimeBodyPart attachPart = new MimeBodyPart();
        attachPart.setDataHandler(new DataHandler(new ByteArrayDataSource(is, mimeType)));
        attachPart.setFileName(name);
        attachments.add(attachPart);
        return this;
    }

    /**
     * Builds the {@code MimeMessage}.
     * This method can be called only once.
     *
     * @return The create {@code MimeMessage}.
     */
    public MimeMessage build() throws MessagingException {
        checkState();

        MimeMultipart mp = new MimeMultipart();
        mp.addBodyPart(body);

        for (MimeBodyPart mbp : attachments) {
            mp.addBodyPart(mbp);
        }

        MimeMessage localMessage = message;
        localMessage.setContent(mp);

        message = null;
        return localMessage;
    }

    /**
     * Builds and send the message.
     * This method can be called only once.
     *
     * @return The sent message.
     */
    public MimeMessage send() throws MessagingException {
        MimeMessage localMessage = build();
        Transport.send(localMessage);
        return localMessage;
    }

    /**
     * Builds and send the message.
     * This method can be called only once.
     *
     * @return The sent message.
     */
    public MimeMessage send(String username, String password) throws MessagingException {
        MimeMessage localMessage = build();
        Transport.send(localMessage, username, password);
        return localMessage;
    }

    private void checkState() {
        if (message == null) {
            throw new IllegalStateException("Message already built");
        }
    }

    /**
     * Parses the {@code addressList} and reassign the personal part of each one
     * with UTF-8 encoding, so accents are not lost during transfer.
     *
     * @param addressList The comma-separated address list.
     * @return The parsed address list.
     */
    protected List<InternetAddress> parseAddressList(String... addressList) throws AddressException {
        List<InternetAddress> addrs = new ArrayList<>();
        for (String addrLst : addressList) {
            for (InternetAddress addr : InternetAddress.parse(addrLst)) {
                String personal = addr.getPersonal();
                if (personal != null) {
                    //this is a hack to handle correctly the personal part of addresses
                    //when there are accents
                    try {
                        addr.setPersonal(personal);
                    } catch (UnsupportedEncodingException uee) { /* ignored */ }
                }
                addrs.add(addr);
            }
        }
        return addrs;
    }
}
