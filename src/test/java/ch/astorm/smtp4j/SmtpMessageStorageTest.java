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

package ch.astorm.smtp4j;

import ch.astorm.smtp4j.core.DefaultSmtpMessageHandler;
import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.core.SmtpMessageHandler.SmtpMessageReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SmtpMessageStorageTest {
    private static final int NB_MESSAGES = 250;

    @Test
    public void testIterator() throws Exception {
        try (ExecutorService service = Executors.newCachedThreadPool()) {

            Future<Integer> receiver;
            try (SmtpServer smtpServer = new SmtpServerBuilder().withPort(1025).start()) {
                Callable<Integer> msgReader = () -> {
                    int counter = 0;
                    try (SmtpMessageReader reader = smtpServer.receivedMessageReader()) {
                        SmtpMessage msg = reader.readMessage();
                        while (msg != null) {
                            ++counter;
                            msg = reader.readMessage();
                        }
                    }
                    return counter;
                };
                receiver = service.submit(msgReader);

                List<Future<Void>> senders = new ArrayList<>(NB_MESSAGES);
                for (int i = 0; i < NB_MESSAGES; ++i) {
                    int idx = i;
                    Callable<Void> v = () -> {
                        int rnd = (int) (Math.random() * 900) + 100;
                        Thread.sleep(rnd);

                        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer);
                        messageBuilder.from("from@local.host")
                                .to("target1to@local.host")
                                .subject("Test " + idx)
                                .body("Some simple message");
                        messageBuilder.send();
                        return null;
                    };
                    senders.add(service.submit(v));
                }

                for (Future<Void> future : senders) {
                    future.get();
                }

                //wait for the last sockets to be handled by the receiver
                Thread.sleep(500);
            }

            Integer receivedMessages = receiver.get();
            assertEquals(NB_MESSAGES, receivedMessages);
            service.shutdown();
        }
    }

    @Test
    public void testVirtualThread() throws Exception {
        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> receiver;
            try (SmtpServer smtpServer = new SmtpServerBuilder()
                    .withPort(1025)
                    .withExecutorService(service)
                    .start()) {
                Callable<Integer> msgReader = () -> {
                    int counter = 0;
                    try (SmtpMessageReader reader = smtpServer.receivedMessageReader()) {
                        SmtpMessage msg = reader.readMessage();
                        while (msg != null) {
                            ++counter;
                            msg = reader.readMessage();
                        }
                    }
                    return counter;
                };
                receiver = service.submit(msgReader);

                List<Future<Void>> senders = new ArrayList<>(NB_MESSAGES);
                for (int i = 0; i < NB_MESSAGES; ++i) {
                    int idx = i;
                    Callable<Void> v = () -> {
                        int rnd = (int) (Math.random() * 900) + 100;
                        Thread.sleep(rnd);

                        MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer);
                        messageBuilder.from("from@local.host")
                                .to("target1to@local.host")
                                .subject("Test " + idx)
                                .body("Some simple message");
                        messageBuilder.send();
                        return null;
                    };
                    senders.add(service.submit(v));
                }

                for (Future<Void> future : senders) {
                    future.get();
                }

                //wait for the last sockets to be handled by the receiver
                Thread.sleep(500);
            }

            Integer receivedMessages = receiver.get();
            assertEquals(NB_MESSAGES, receivedMessages);

            service.shutdown();
        }
    }


    @Test
    public void testNonStartedIterator() throws Exception {
        DefaultSmtpMessageHandler store = new DefaultSmtpMessageHandler();
        assertNull(store.messageReader().readMessage());
    }

    @Test
    public void testClosedIterator() throws Exception {
        DefaultSmtpMessageHandler store = new DefaultSmtpMessageHandler();
        try (SmtpServer smtpServer = new SmtpServerBuilder().withPort(1025).withMessageHandler(store).withListener(store).start()) {
            /* nothing */
        }
        assertNull(store.messageReader().readMessage());
    }
}
