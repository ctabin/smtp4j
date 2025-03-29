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

package ch.astorm.smtp4j.protocol;

import java.util.List;

/**
 * Protocol exchange data between smtp4j and the sender.
 */
public class SmtpExchange {
    private final List<String> received;
    private final String replied;
    
    public SmtpExchange(List<String> received, String replied) {
        this.received = received;
        this.replied = replied;
    }
    
    /**
     * Returns the data received by smtp4j.
     * Most of the messages will only contain one item. In the case of the {@link SmtpCommand.Type#DATA}
     * command, there might be multiple items (each item corresponds to one line in the SMTP protocol).
     *
     * @return The received data.
     */
    public List<String> getReceivedData() {
        return received;
    }
    
    /**
     * Returns the response sent back by smtp4j:
     *
     * @return The response data.
     */
    public String getRepliedData() {
        return replied;
    }
}
