
package ch.astorm.smtp4j.secure;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Simple implementation of a {@link SSLContextProvider}.
 */
public class DefaultSSLContextProvider implements SSLContextProvider {
    private final KeyManagerFactory keyManagerFactory;
    
    public DefaultSSLContextProvider(KeyManagerFactory kmFactory) {
        this.keyManagerFactory = kmFactory;
    }
    
    /**
     * Creates a new {@code SSLContextProvider} with the given {@code keyStore} and {@code password}.
     *
     * @param keyStore The {@code KeyStore}.
     * @param password The password.
     * @return A new {@code DefaultSSLContextProvider} instance.
     */
    public static DefaultSSLContextProvider create(KeyStore keyStore, char[] password) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        return new DefaultSSLContextProvider(keyManagerFactory);
    }
    
    /**
     * Creates a new {@code SSLContextProvider} that uses and internal generated keystore.
     * This provides an easy-for-use tool for testing TLS communications.
     * This implies that the Session properties {@code mail.smtp.ssl.checkserveridentity} and {@code mail.smtp.ssl.trust}
     * must be set to {@code false} and {@code *} respectively.
     *
     * @return A new {@code DefaultSSLContextProvider} instance.
     */
    public static DefaultSSLContextProvider selfSigned() throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, IOException, CertificateException {
        char[] password = "smtp4j".toCharArray();
        
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try(InputStream is = DefaultSSLContextProvider.class.getResourceAsStream("keystore.jks")) {
            keyStore.load(is, password);
        }
        
        return create(keyStore, password);
    }
    
    @Override
    public SSLContext getSSLContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }
}
