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

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class LoginAuthFlow implements AuthFlow {
    private final static SecureRandom random;

    static {
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final SmtpTransactionHandler transactionHandler;

    private int state = 0;
    private String username;

    private final String USERNAME = Base64.getEncoder().encodeToString("Username:".getBytes(StandardCharsets.US_ASCII));
    private final String PASSWORD = Base64.getEncoder().encodeToString("Password:".getBytes(StandardCharsets.US_ASCII));

    public LoginAuthFlow(SmtpTransactionHandler transactionHandler) {
        this.transactionHandler = transactionHandler;
    }

    @Override
    public State handle() throws SmtpProtocolException {
        switch (state) {
            case 0 -> {
                // request username
                transactionHandler.reply(SmtpProtocolConstants.CODE_SERVER_CHALLENGE, USERNAME);
                state++;
                return State.CONTINUE;
            }
            case 1 -> {
                username = new String(Base64.getDecoder().decode(transactionHandler.nextLine()), StandardCharsets.UTF_8);

                // request password
                transactionHandler.reply(SmtpProtocolConstants.CODE_SERVER_CHALLENGE, PASSWORD);
                state++;
                return State.CONTINUE;
            }
            case 2 -> {
                byte[] password = Base64.getDecoder().decode(transactionHandler.nextLine());

                if (Arrays.equals(password,
                        transactionHandler.getAuth().getPasswordForUser(username))) {
                    transactionHandler.reply(SmtpProtocolConstants.CODE_AUTH_OK, "OK");
                    return State.AUTHENTICATED;
                }
            }
        }

        transactionHandler.reply(SmtpProtocolConstants.CODE_AUTH_FAILED, "Authentication failed");
        return State.FAILED;
    }
}
