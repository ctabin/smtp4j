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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LineAwareBufferedInputStream extends BufferedInputStream {
    private final byte[] buffer = new byte[1000];

    public LineAwareBufferedInputStream(InputStream in) {
        super(in);
    }

    public byte[] readLine() throws IOException {
        for (int i = 0; i < buffer.length; i++) {
            int c = super.read();
            if (c == -1) {
                // EOF
                return i == 0 ? null : ByteArrayUtils.copy(buffer, i);
            }
            buffer[i] = (byte) c;
            if (hasLineEnd(buffer, i)) {
                // NEWLINE
                return ByteArrayUtils.copy(buffer, i - 1);
            }
        }
        return buffer;
    }

    private boolean hasLineEnd(byte[] line, int i) {
        if (i < 1) {
            return false;
        }
        boolean ret = line[i] == '\n'
                && line[i - 1] == '\r';
        return ret;
    }
}
