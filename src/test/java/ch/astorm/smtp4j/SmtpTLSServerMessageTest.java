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
import ch.astorm.smtp4j.util.MimeMessageBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmtpTLSServerMessageTest {
    private static SmtpServer smtpServer;

    @BeforeAll
    public static void init() throws Exception {
        smtpServer = new SmtpServerBuilder()
                .withPort(1025)
                .withSecure(() -> {
                    try {
                        KeyStore keyStore = KeyStore.getInstance("JKS");
                        keyStore.load(LocalServer.class.getResourceAsStream("/smtpserver.jks"), "changeit".toCharArray());

                        // Initialize the SSL context with the keystore
                        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        keyManagerFactory.init(keyStore, "changeit".toCharArray());

                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
                        return sslContext;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .start();
    }

    @AfterAll
    public static void after() throws Exception {
        smtpServer.close();
    }

    @Test
    public void testSend() throws Exception {
        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer, true)
                .from("testSend@smtp4j.local")
                .to("target@smtp4j.local")
                .subject("Test simple message 1")
                .body("Test simple message 1");

        messageBuilder.send();

        List<SmtpMessage> received = smtpServer.readReceivedMessages();
        assertEquals(1, received.size());
        assertTrue(received.getFirst().isSecure());
    }
}
