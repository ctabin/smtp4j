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

import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.core.SmtpMessageHandler;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalServer {
    public static void main(String[] args) throws Exception {
        try (var server = new SmtpServerBuilder()
                .withMaxMessageSize(1024 * 1024)
                .withPort(2525)
                .withMessageHandler(new SmtpMessageHandler() {
                    @Override
                    public SmtpMessageReader messageReader() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public List<SmtpMessage> readMessages(long delayIfNoMessage, TimeUnit unit) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void notifyMessage(SmtpServer server, SmtpMessage smtpMessage) {
                        // handled by server listener
                    }
                })
                .withListener((smtpServer, smtpMessage) -> {
                    try {
                        MimeMessage mimeMessage = smtpMessage.getMimeMessage();
                        System.out.println("From: " + Stream.of(notNull(mimeMessage.getFrom()))
                                .map(Address::toString)
                                .collect(Collectors.joining(", ")));
                        System.out.println("To: " + Stream.of(notNull(mimeMessage.getRecipients(Message.RecipientType.TO)))
                                .map(Address::toString)
                                .collect(Collectors.joining(", ")));
                        System.out.println("Cc: " + Stream.of(notNull(mimeMessage.getRecipients(Message.RecipientType.CC)))
                                .map(Address::toString)
                                .collect(Collectors.joining(", ")));
                        System.out.println("Bcc: " + Stream.of(notNull(mimeMessage.getRecipients(Message.RecipientType.BCC)))
                                .map(Address::toString)
                                .collect(Collectors.joining(", ")));
                        System.out.println("Subject: " + mimeMessage.getSubject());
                        System.out.println("Message\n" + mimeMessage.getContent());
                        System.out.println("<END OF MESSAGE>\n");
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (smtpMessage.getSubject()
                            .contains("kill")) {

                        Thread.startVirtualThread(() -> {
                            try {
                                smtpServer.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                })
                .build()) {
            server.start();

            while (server.isRunning()) {
                Thread.sleep(1000);
            }
        }
    }

    private static Address[] notNull(Address[] addresses) {
        if (addresses != null) {
            return addresses;
        }
        return new Address[0];
    }
}
