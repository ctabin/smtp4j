
package ch.astorm.smtp4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class SmtpServerTest {

    @Test
    public void testDynamicPortLookup() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try(SmtpServer server1 = builder.start();
            SmtpServer server2 = builder.start();
            SmtpServer server3 = builder.start()) {
            assertNotEquals(server1.getPort(), server2.getPort());
            assertNotEquals(server1.getPort(), server3.getPort());
            assertNotEquals(server2.getPort(), server3.getPort());
        }
    }

    @Test
    public void testMultipleStartClose() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        try(SmtpServer server = builder.start()) {
            assertNotNull(server.getMessageHandler());
            assertThrows(IllegalStateException.class, () -> server.start());
            server.close();
        }
    }

    @Test
    public void testMultipleStart() throws Exception {
        SmtpServerBuilder builder = new SmtpServerBuilder();
        SmtpServer server = builder.build();
        assertTrue(server.isClosed());

        server.start();
        assertFalse(server.isClosed());
        int port = server.getPort();
        server.close();

        assertTrue(server.isClosed());

        server.start();
        assertFalse(server.isClosed());
        assertEquals(port, server.getPort());
        server.close();

        assertTrue(server.isClosed());
    }
}
