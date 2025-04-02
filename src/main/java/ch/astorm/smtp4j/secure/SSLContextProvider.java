
package ch.astorm.smtp4j.secure;

import javax.net.ssl.SSLContext;

/**
 * {@code SSLContext} provider.
 */
public interface SSLContextProvider {
    /**
     * Returns an {@code SSLContext}.
     */
    SSLContext getSSLContext() throws Exception;
}
