
package ch.astorm.smtp4j;

import ch.astorm.smtp4j.core.SmtpMessage;
import ch.astorm.smtp4j.core.DefaultSmtpMessageHandler;
import ch.astorm.smtp4j.util.MimeMessageBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import ch.astorm.smtp4j.core.SmtpMessageHandler.SmtpMessageReader;

public class SmtpMessageStorageTest {
    private static final int NB_MESSAGES = 250;
    
    @Test
    public void testIterator() throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(20); //cached thread pool results sometimes in hanging test

        Future<Integer> receiver;
        try(SmtpServer smtpServer = new SmtpServerBuilder().withPort(1025).start()) {
            Callable<Integer> msgReader = () -> {
                int counter = 0;
                try(SmtpMessageReader reader = smtpServer.receivedMessageReader()) {
                    SmtpMessage msg = reader.readMessage();
                    while(msg!=null) {
                        ++counter;
                        System.out.println("Received message counter: "+counter);
                        msg = reader.readMessage();
                    }
                }
                return counter;
            };
            receiver = service.submit(msgReader);

            List<Future<Void>> senders = new ArrayList<>(NB_MESSAGES);
            for(int i=0 ; i<NB_MESSAGES ; ++i) {
                int idx = i;
                Callable<Void> v = () -> {
                    int rnd = (int)(Math.random()*900)+100;
                    Thread.sleep(rnd);
                    
                    MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer);
                    messageBuilder.from("from@local.host").
                                   to("target1to@local.host").
                                   subject("Test "+idx).
                                   body("Some simple message");
                    messageBuilder.send();
                    return null;
                };
                senders.add(service.submit(v));
            }

            for(Future<Void> future : senders) {
                future.get();
            }
            
            //wait for the last sockets to be handled by the receiver
            Thread.sleep(500);
        }

        Integer receivedMessages = receiver.get();
        assertEquals(NB_MESSAGES, receivedMessages);
        service.shutdown();
    }

    @Test
    public void testNonStartedIterator() throws Exception {
        DefaultSmtpMessageHandler store = new DefaultSmtpMessageHandler();
        assertNull(store.messageReader().readMessage());
    }

    @Test
    public void testClosedIterator() throws Exception {
        DefaultSmtpMessageHandler store = new DefaultSmtpMessageHandler();
        try(SmtpServer smtpServer = new SmtpServerBuilder().withPort(1025).withMessageHandler(store).withListener(store).start()) {
            /* nothing */
        }
        assertNull(store.messageReader().readMessage());
    }
}
