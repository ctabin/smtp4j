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

import ch.astorm.smtp4j.util.StringUtils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

public class CramMd5AuthFlow implements AuthFlow {
    private final static SecureRandom random;

    static {
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final SmtpTransactionHandler transactionHandler;

    private String currentAuthChallenge;

    public CramMd5AuthFlow(SmtpTransactionHandler transactionHandler) {
        this.transactionHandler = transactionHandler;
    }

    @Override
    public State handle() throws SmtpProtocolException {
        if (currentAuthChallenge == null) {
            // Step 1: send the challenge
            sendChallenge();
            return State.CONTINUE;
        }

        // Step 2: process login
        return processLogin();
    }

    private State processLogin() throws SmtpProtocolException {
        String[] credentials = StringUtils.decode(transactionHandler.nextLine()).split(" ", 2);
        if (credentials.length != 2) {
            transactionHandler.reply(SmtpProtocolConstants.CODE_AUTH_FAILED, "Authentication failed");
            return State.FAILED;
        }

        String user = credentials[0];
        String pass = credentials[1];
        if (Objects.equals(pass,
                StringUtils.hashWithHMACMD5(
                        currentAuthChallenge,
                        transactionHandler.getAuth().getPasswordForUser(user)))) {
            transactionHandler.reply(SmtpProtocolConstants.CODE_AUTH_OK, "OK");
            return State.AUTHENTICATED;
        }

        transactionHandler.reply(SmtpProtocolConstants.CODE_AUTH_FAILED, "Authentication failed");
        return State.FAILED;
    }

    private void sendChallenge() {
        currentAuthChallenge = String.format("<%d.%d@%s>",
                random.nextLong(),
                System.currentTimeMillis(),
                transactionHandler.getSmtpServer().getLocalHostname());
        transactionHandler.reply(SmtpProtocolConstants.CODE_SERVER_CHALLENGE, StringUtils.encode(currentAuthChallenge));
    }
}
