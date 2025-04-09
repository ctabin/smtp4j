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

import ch.astorm.smtp4j.util.ByteArrayUtils;
import ch.astorm.smtp4j.util.StringUtils;

import java.nio.charset.StandardCharsets;

public class PlainAuthFlow implements AuthFlow {
    private final SmtpTransactionHandler transaction;
    private final String[] authTokens;

    public PlainAuthFlow(SmtpTransactionHandler transaction, String[] authTokens) {
        this.transaction = transaction;
        this.authTokens = authTokens;
    }

    @Override
    public State handle() {
        if (authTokens.length != 2) {
            transaction.reply(SmtpProtocolConstants.CODE_COMMAND_PARAMETERS_INVALID, "Invalid parameters");
            return State.FAILED;
        }

        String login = authTokens[1];
        String[] credentials = StringUtils.decode(login).split("\\x00", 3);
        if (credentials.length != 3) {
            transaction.reply(SmtpProtocolConstants.CODE_COMMAND_PARAMETERS_INVALID, "Invalid parameters");
            return State.FAILED;
        }
        String user = credentials[1];
        String pass = credentials[2];
        if (ByteArrayUtils.equals(
                pass.getBytes(StandardCharsets.UTF_8),
                transaction.getAuth().getPasswordForUser(user))) {
            transaction.reply(SmtpProtocolConstants.CODE_AUTH_OK, "OK");
            return State.AUTHENTICATED;
        }

        transaction.reply(SmtpProtocolConstants.CODE_AUTH_FAILED, "Authentication failed");
        return State.FAILED;
    }
}
