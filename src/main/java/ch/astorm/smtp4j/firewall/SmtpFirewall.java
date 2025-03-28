package ch.astorm.smtp4j.firewall;

import java.io.InputStream;
import java.net.InetAddress;

public interface SmtpFirewall {
    default boolean accept(InetAddress inetAddress) {
        return true;
    }

    default InputStream firewallInputStream(InputStream inputStream) {
        return inputStream;
    }

    default boolean isAllowedFrom(String mailFrom) {
        return true;
    }

    default boolean isAllowedRecipient(String recipient) {
        return true;
    }

    default boolean isAllowedMessage(String message) {
        return true;
    }
}
