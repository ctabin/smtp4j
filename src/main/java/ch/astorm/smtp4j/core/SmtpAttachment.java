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

package ch.astorm.smtp4j.core;

import jakarta.mail.MessagingException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Attachment of an {@code SmtpMessage}.
 */
public class SmtpAttachment {
    private final String filename;
    private final String contentType;
    private StreamProvider streamProvider;

    /**
     * Provides an {@code InputStream} to read the attachment content.
     */
    @FunctionalInterface
    public interface StreamProvider {

        /**
         * Returns a new {@code InputStream}.
         * The behavior is implementation-dependent.
         *
         * @return A {@code InputStream} instance.
         */
        InputStream openStream() throws IOException, MessagingException;
    }

    /**
     * Creates a new {@code SmtpAttachement} with the specified parameters.
     *
     * @param filename       The attachment's name.
     * @param contentType    The Content Type.
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
        if (streamProvider == null) {
            throw new IOException("stream not available");
        }

        InputStream is = streamProvider.openStream();
        streamProvider = null;
        return is;
    }
}
