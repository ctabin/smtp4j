[![Maven](https://img.shields.io/maven-central/v/ch.astorm/smtp4j.svg)](https://search.maven.org/search?q=g:ch.astorm%20AND%20a:smtp4j)
[![Build](https://app.travis-ci.com/ctabin/smtp4j.svg?branch=master)](https://app.travis-ci.com/github/ctabin/smtp4j/branches)

# smtp4j

Simple API to fake an SMTP server for Unit testing (and more).

## About this project

This API is inspired from [dumbster](https://github.com/kirviq/dumbster) with the following improvements:
- Dynamic port lookup feature
- Support of MIME messages with attachments
- Up-to-date dependencies
- Extended tests
- Numerous bugfixes


Here is the compatibility map of this API:

| Version  | JDK                | Package
| -------- | ------------------ | ---------
| <= 1.2.2 | JDK 8 and upwards  | `javax`
| >= 2.0.0 | JDK 11 and upwards | `jakarta`


## Installation (maven)

Use the following dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>ch.astorm</groupId>
    <artifactId>smtp4j</artifactId>
    <version>3.0.1</version>
</dependency>
```

## Quick Start Guide

Here is a quick example of the usage of this API that shows an oversight on
how it can be used:

```java
/* SMTP server is started on port 1025 */
SmtpServerBuilder builder = new SmtpServerBuilder();
try(SmtpServer server = builder.withPort(1025).start()) {
    
    /* create and send an SMTP message to smtp4j */
    MimeMessageBuilder messageBuilder = new MimeMessageBuilder(server);
    messageBuilder.from("source@smtp4j.local").
                   to("target1@smtp4j.local", "John Doe <john@smtp4j.local>").
                   cc("target3@smtp4j.local").
                   subject("Hello, world !").
                   body("Hello\r\nGreetings from smtp4j !\r\n\r\nBye.").
                   attachment("data.txt", new File("someAttachment.txt"));
                   
    messageBuilder.send(); //uses Transport.send(...)

    /* retrieve the sent message in smtp4j */
    List<SmtpMessage> messages = server.readReceivedMessages();
    assertEquals(1, messages.size());
    
    /* analyze the content of the message */
    SmtpMessage receivedMessage = messages.get(0);
    String from = receivedMessage.getFrom();
    String subject = receivedMessage.getSubject();
    String body = receivedMessage.getBody();
    Date sentDate = receivedMessage.getSentDate();
    List<String> recipientsTo = receivedMessage.getRecipients(RecipientType.TO);
    List<SmtpAttachment> attachments = receivedMessage.getAttachments();
}
```

## Usage

Here are some usages about specific parts of the API. For more examples,
look in the [tests](src/test/java/ch/astorm/smtp4j).

Basically, it is recommended to always use the [SmtpServerBuilder](src/main/java/ch/astorm/smtp4j/SmtpServerBuilder.java)
class to instanciate a new `SmtpServer` instance.

### SMTP server port

The `SmtpServer` can be configured either to listen to a specific port or to find
dynamically a free port to listen to.

A static port can simply be specified like this:

```java
SmtpServerBuilder builder = new SmtpServerBuilder();
try(SmtpServer server = builder.withPort(1025).start()) {
    //server is listening on port 1025
}
```

On the other hand, if no port is defined, the `SmtpServer` will find a free port
to listen to when it is started:

```java
SmtpServerBuilder builder = new SmtpServerBuilder();
try(SmtpServer server = builder.start()) {
    int port = server.getPort(); //port listen by the server
}
```

When the port is not specified, the `SmtpServer` will try to open a server socket on
the default SMTP port (25). If the latter fails (most probably), it will look up for
a free port starting from 1024.

Note that generally ports under 1024 can only be used with root privileges.

### Session

The `SmtpServer` provides some utilities that let you create a new `Session`
for message creation and sending. The latter will be automatically connected
to the running server (on localhost):

```java
SmtpServerBuilder builder = new SmtpServerBuilder();
try(SmtpServer server = builder.start()) {
    Session session = server.createSession();
    //use the session to create a MimeMessage
}
```

### Received messages

The received messages can be accessed directly through the `SmtpServer`:

```java
List<SmtpMessage> receivedMessages = smtpServer.readReceivedMessages();
```

This method will clear the server's storage cache, hence another invocation of
the same method will yield an empty list until a new message has been received.

**WARNING:** Do not use this method concurrently `SmtpServer.receivedMessageReader()`
because of race conditions.

Since smtp4j is multithreaded, it may happen that there is not enough time to process the message before
reading it. This can be easily circumvented by defining a delay to wait when there is no message yet received.

```
List<SmtpMessage> receivedMessages = smtpServer.readReceivedMessages(2, TimeUnit.SECONDS);
```

#### Waiting for messages

A simple API is provided to wait and loop over the received messages:

```java
try(SmtpMessageReader reader = smtpServer.receivedMessageReader()) {
    SmtpMessage smtpMessage = reader.readMessage(); //blocks until the first message is available
    while(smtpMessage!=null) {
        /* ... */
      
        //blocks until the next message is available
        smtpMessage = reader.readMessage();
    }
}
```

When the `SmtpServer` is closed, the reader will yield `null`.

**WARNING:** Creating multiple instances of `SmtpMessageReader` will cause a race condition between
them and hence, a message will be received only by one of the readers. For the same reasons, do not use
`SmtpServer.readReceivedMessages()` when using a reader.

#### SMTP messages

The API of `SmtpMessage` provides an easy access to all the basic fields:

```java
String from = smtpMessage.getFrom();
String subject = smtpMessage.getSubject();
String body = smtpMessage.getBody();
Date sentDate = smtpMessage.getSentDate();
List<String> recipientsTo = smtpMessage.getRecipients(RecipientType.TO);
List<SmtpAttachment> attachments = smtpMessage.getAttachments();
```

It is also possible to retrieve some data directly issued from the underlying SMTP exchanges
between the server and the client. Those data might differ (even be missing) from the resulting
`MimeMessage`:

```java
String sourceFrom = smtpMessage.getSourceFrom();
List<String> sourceRecipients = smtpMessage.getSourceRecipients();
```

Typically, the `BCC` recipients will be absent from the `MimeMessage` but will
be available through the `getSourceRecipients()` method.

If more specific data has to be accessed, it is possible to retrieve the raw
data with the following methods:

```java
MimeMessage mimeMessage = smtpMessage.getMimeMessage();
String mimeMessageStr = smtpMessage.getRawMimeContent();
```

#### Low level SMTP exchanges

One can access direclty the exchanges between the sender and smtp4j.

```java
List<SmtpExchange> exchanges = smtpMessage.getSmtpExchanges();
List<String> receivedData = exchanges.get(0).getReceivedData();
String repliedData = exchanges.get(0).getRepliedData();
```

#### Attachments

Multipart messages might contain many attachments that are accessibles with the `getAttachments()`
method of the `SmtpMessage`. Here is an insight of the `SmtpAttachment` API:

```java
String filename = attachment.getFilename(); // myFile.pdf
String contentType = attachment.getContentType(); // application/pdf; charset=us-ascii; name=myFile.pdf
```

The content of an attachment can be read with the following piece of code:

```java
try(InputStream is = attachment.openStream()) {
    //...
}
```

#### Client-side messages

The API includes a utility class to build SMTP messages from the client side
that can easily be sent to smtp4j. The [MimeMessageBuilder](src/main/java/ch/astorm/smtp4j/util/MimeMessageBuilder.java)
class provides easy-to-use methods to create a Multipart MIME message:

```java
/* SMTP server is started on port 1025 */
SmtpServerBuilder builder = new SmtpServerBuilder();
try(SmtpServer server = builder.withPort(1025).start()) {
    
    /* create and send an SMTP message */
    MimeMessageBuilder messageBuilder = new MimeMessageBuilder(server).
       from("source@smtp4j.local").
       
       //use either multiple arguments
       to("to1@smtp4j.local", "Igôr <to2@smtp4.local>").
       
       //or a comma-separated list
       to("to3@smtp4j.local, My Friend <to4@smtp4j.local>").
       
       //or call the method multiple times
       cc("cc1@smtp4j.local").
       cc("cc2@smtp4j.local").
       
       bcc("bcc@smtp4j.local").
       at("31.12.2020 23:59:59").
       subject("Hello, world !").
       body("Hello\r\nGreetings from smtp4j !\r\n\r\nBye.").
       attachment(new File("file.pdf"));

    //build the message and send it to smtp4j
    messageBuilder.send();

    //process the received message
    //...
}
```

It is also possible to use this builder in a production application by using the
dedicated `Session` constructor:

```java
MimeMessageBuilder messageBuilder = new MimeMessageBuilder(session);
```

#### Server events

It is possible to listen to `SmtpServer` events by implementing a
[SmtpServerListener](src/main/java/ch/astorm/smtp4j/core/SmtpServerListener.java).

```java
SmtpServerListener myListener = new SmtpServerListener() {
    public void notifyStart(SmtpServer server) { System.out.println("Server has been started"); }
    public void notifyClose(SmtpServer server) { System.out.println("Server has been closed"); }
    public void notifyMessage(SmtpServer server, SmtpMessage message) { System.out.println("Message has been received"); }
}

mySmtpServer.addListener(myListener);
```

#### Advanced message handling

By default, once a `SmtpMessage` has been received, it will be stored in a default
[DefaultSmtpMessageHandler](src/main/java/ch/astorm/smtp4j/core/DefaultSmtpMessageHandler.java) instance,
which can be directly accessed like this:

```java
SmtpMessageHandler messageHandler = smtpServer.getMessageHandler();
DefaultSmtpMessageHandler defaultMessageHandler = (DefaultSmtpMessageHandler)messageHandler;
```

It is possible to override this default behavior with your custom handler with the
following piece of code:

```java
SmtpMessageHandler myCustomHandler = new CustomSmtpMessageHandler();

SmtpServerBuilder builder = new SmtpServerBuilder();
try(SmtpServer server = builder.withMessageHandler(myCustomHandler).start()) {
    //...
}
```

### Limitations

For now, it is not possible to communicate securely (SMTPS or SSL/TLS) through this API. If the client
tries to initiate a secure channel, the connection will be closed.

## Donate

This project is completely developed during my spare time.

Since I'm a big fan of cryptocurrencies and especially [Cardano](https://cardano.org) (ADA), you can send me
some coins at the address below (check it [here](https://cardanoscan.io/address/addr1q9sgms4vc038nq7hu4499yeszy0rsq3hjeu2k9wraksle8arg0n953hlsrtdzpfnxxw996l4t6qu5xsx8cmmakjcqhksaqpj66)):

```
addr1q9sgms4vc038nq7hu4499yeszy0rsq3hjeu2k9wraksle8arg0n953hlsrtdzpfnxxw996l4t6qu5xsx8cmmakjcqhksaqpj66
```
