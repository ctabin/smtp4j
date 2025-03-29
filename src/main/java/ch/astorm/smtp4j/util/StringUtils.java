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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

public final class StringUtils {
    private final static String[] EMPTY_STRING_ARRAY = new String[0];

    private StringUtils() {
    }

    public static String[] split(String text, String regex, int count) {
        if (text == null) {
            return EMPTY_STRING_ARRAY;
        }
        return text.split(regex, count);
    }

    public static String toUpperCase(String text) {
        if (text == null) {
            return null;
        }
        return text.toUpperCase(Locale.ROOT);
    }

    public static String decode(String text) {
        String ret = new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
        return ret;
    }

    public static String encode(String text) {
        String ret = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        return ret;
    }

    public static String decode(byte[] bytes) {
        String ret = new String(Base64.getDecoder().decode(bytes), StandardCharsets.UTF_8);
        return ret;
    }

    public static String hashWithHMACMD5(String string, byte[] key) {
        String cryptedString;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "HmacMD5");
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(keySpec);

            byte[] bytes = mac.doFinal(string.getBytes(StandardCharsets.UTF_8));

            StringBuilder hash = new StringBuilder();
            for (byte aByte : bytes) {
                String hex = Integer.toHexString(0xFF & aByte);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            cryptedString = hash.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cryptedString;
    }
}
