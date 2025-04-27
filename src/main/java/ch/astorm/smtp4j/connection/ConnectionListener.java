
package ch.astorm.smtp4j.connection;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Connection listener.
 */
public interface ConnectionListener {
    
    /**
     * This method is invoked when a new {@code Socket} is connected, prior to any
     * protocol exchange.
     * <p>One can use this interface to implement a simple Firewall to prevent connections
     * from a given host just by throwing an exception.</p>
     *
     * @param remoteHost The remote host connected.
     */
    void connected(InetAddress remoteHost) throws IOException;
}
