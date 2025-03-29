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
