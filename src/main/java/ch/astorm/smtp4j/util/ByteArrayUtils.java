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

public final class ByteArrayUtils {
    private final static byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private ByteArrayUtils() {
    }

    public static byte[] copy(byte[] line, int length) {
        if (length == 0) {
            return EMPTY_BYTE_ARRAY;
        }

        byte[] ret = new byte[length];
        System.arraycopy(line, 0, ret, 0, length);
        return ret;
    }

    public static boolean equals(byte[] buf1, byte[] buf2) {
        if (buf1 == buf2) {
            return true;
        }
        if (buf1 == null || buf2 == null) {
            return false;
        }
        if (buf1.length != buf2.length) {
            return false;
        }
        for (int i = 0; i < buf1.length; i++) {
            if (buf1[i] != buf2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean startsWith(byte[] buf1, byte[] startsWith) {
        if (buf1 == startsWith) {
            return true;
        }
        if (buf1 == null || startsWith == null) {
            return false;
        }
        if (buf1.length < startsWith.length) {
            return false;
        }
        for (int i = 0; i < startsWith.length; i++) {
            if (buf1[i] != startsWith[i]) {
                return false;
            }
        }
        return true;
    }
}
