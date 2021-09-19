
package ch.astorm.smtp4j.core;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Attachment of an {@code SmtpMessage}.
 */
public class SmtpAttachment {
    private String filename;
    private String contentType;
    private StreamProvider streamProvider;

    /**
     * Provides an {@code InputStream} to read the attachment content.
     */
    @FunctionalInterface
    public static interface StreamProvider {
        InputStream openStream() throws IOException, MessagingException;
    }

    /**
     * Creates a new {@code SmtpAttachement} with the specified parameters.
     *
     * @param filename The attachment's name.
     * @param contentType The Content Type.
     * @param streamProvider The stream provider.
     */
    public SmtpAttachment(String filename, String contentType, StreamProvider streamProvider) {
        this.filename = filename;
        this.contentType = contentType;
        this.streamProvider = streamProvider;
    }

    /**
     * Returns the file name.
     * 
     * @return The file name.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns the Content Type.
     * Generally, this will look like <pre>text/plain; charset=us-ascii; name=file.txt</pre>.
     *
     * @return The Content TYpe.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Opens a new {@code InputStream} on this attachment.
     * This method can only be called once.
     *
     * @return A new {@code InputStream}.
     */
    public InputStream openStream() throws IOException, MessagingException {
        if(streamProvider==null) { throw new IOException("stream not available"); }

        InputStream is = streamProvider.openStream();
        streamProvider = null;
        return is;
    }
}
