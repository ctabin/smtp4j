[![Maven](https://img.shields.io/maven-central/v/ch.astorm/smtp4j.svg)](https://search.maven.org/search?q=g:ch.astorm%20AND%20a:smtp4j)
[![Build](https://app.travis-ci.com/ctabin/smtp4j.svg?branch=master)](https://app.travis-ci.com/github/ctabin/smtp4j/branches)
[![javadoc](https://javadoc.io/badge2/ch.astorm/smtp4j/javadoc.svg)](https://javadoc.io/doc/ch.astorm/smtp4j) 

# smtp4j

Simple API to fake an SMTP server for Unit testing (and more).

## About this project

This API is inspired from [dumbster](https://github.com/kirviq/dumbster) and provides the following features:
- [Dynamic port lookup](#smtp-server-port).
- Support of MIME messages with [attachments](#attachments).
- Support of secure channel communication ([`SMTPS`](#secure-channel-smtps) and [`STARTTLS`](#switch-to-secure-channel-starttls)).
- Support of `PLAIN`,`LOGIN` and `CRAM-MD5` [authentication schemes](#authentication-schemes).
- Support of [message size limit](#message-size-limit).
- Support of [connection listeners](#connection-listener) (firewall).
- Access to SMTP [exchanges](#low-level-smtp-exchanges).
- Improved [multi-threading](#multithreading) support.
- Up-to-date dependencies.
- Extended unit tests.

Here is the compatibility map of this API:

| Version  | JDK                | Package
| -------- | ------------------ | ---------
| <= 1.2.2 | JDK 8 and upwards  | `javax`
| >= 2.0.0 | JDK 11 and upwards | `jakarta`
| >= 3.0.0 | JDK 11 and upwards | `jakarta`
| >= 4.0.0 | JDK 21 and upwards | `jakarta`

## Installation (maven)

Use the following dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>ch.astorm</groupId>
    <artifactId>smtp4j</artifactId>
    <version>4.1.0</version>
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

#### Refuse a message

It is possible to trigger message refusal through the API. The exception message will
be received on the SMTP client side.

```java
SmtpServerBuilder builder = new SmtpServerBuilder();
try(SmtpServer server = builder.start()) {
    server.addListener((srv, msg) -> {
        throw new IllegalStateException("Message refused");
    });
    
    try {
        new MimeMessageBuilder(server).
            to("test@astorm.ch").
            subject("Test").
            body("Hello!").
            send();
    } catch(MessagingException e) {
        String message = e.getMessage(); //554 Message refused
    }
}
```

#### Message storage

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

#### Message size limit

It is possible configure smtp4j to reject messages that exceed a given size.

```java
try(SmtpServer server = builder.withMaxMessageSize(1024).start()) {
  //...
}
```

### Secure channel (SMTPS)

By default, the `SMTP` protocol is used, which is not encrypted. To use `SMTPS` instead, use
the following code snippet:

```java
try(SmtpServer smtpServer = new SmtpServerBuilder().
      withProtocol(Protocol.SMTPS).
      withSSLContextProvider(DefaultSSLContextProvider.selfSigned()).
      withPort(1025).
      start()) {
  MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer).
      from("source@smtp4j.local").
      to("target@smtp4j.local").
      subject("Subject").
      body("Message");
  messageBuilder.send();
}
```

**Note**: it is yet not possible to have both `STMP` and `STMPS` protocols enabled at the same time in the same `SmtpServer`.
You'll have to create two different instances for each protocol or allow the `STARTTLS` command (see below).

When the `SMTPS` protocol is used, the following SMTP properties are set when creating a new
`Session`:

| Property | Value |
| -------- | ----- |
| `mail.transport.protocol` | `smtps` |
| `mail.transport.protocol.rfc822` | `smtps` |
| `mail.smtps.host` | `localhost` |
| `mail.smtps.port` | `<port>` |
| `mail.smtps.ssl.checkserveridentity` | `false` |
| `mail.smtps.ssl.trust` | `*` |

#### Switch to secure channel (STARTTLS)

The SMTP support the `STARTTLS` command once a client session is initiated. Also the API provides a self-signed
certificate in order to simulate TLS effectively.

```java
try(SmtpServer smtpServer = new SmtpServerBuilder().
      withStartTLSSupport(true).
      withSSLContextProvider(DefaultSSLContextProvider.selfSigned()).
      withPort(1025).
      start()) {

  //since STARTTLS is supported, the corresponding properties are also set to
  //automatically switch over secure channel communication
  MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer).
      from("source@smtp4j.local").
      to("target@smtp4j.local").
      subject("Subject").
      body("Message");
  messageBuilder.send();
}
```

By default, once TLS support is activated, the `STARTTLS` command is required from the client. It is possible
to still allow plain text exchanges with the following code snipped:

```java
try(SmtpServer smtpServer = new SmtpServerBuilder().
      withStartTLSSupport(true).
      withStartTLSRequired(false).
      withSSLContextProvider(DefaultSSLContextProvider.selfSigned()).
      withPort(1025).
      start()) {
  //...
}
```

When the `STARTTLS` support is enabled, the following SMTP properties are set when creating a new `Session`:

| Property | Value |
| -------- | ----- |
| `mail.transport.protocol` | `smtp` |
| `mail.transport.protocol.rfc822` | `smtp` |
| `mail.smtp.host` | `localhost` |
| `mail.smtp.port` | `<port>` |
| `mail.smtp.starttls.enable` | `true` |
| `mail.smtp.starttls.required` | `true` |
| `mail.smtp.ssl.checkserveridentity` | `false` |
| `mail.smtp.ssl.trust` | `*` |

### Authentication schemes

The `PLAIN`, `LOGIN` and `CRAM-MD5` authentication schemes are natively supported. Once an authentication scheme is added,
it implies that at least one user is declared otherwise all the connections will be rejected.

```java
try(SmtpServer smtpServer = new SmtpServerBuilder().
    withAuthenticator(PlainAuthenticationHandler.INSTANCE).
    withUser("jdoe", "somePassword").
    withPort(1025).
    start()) {

    MimeMessageBuilder messageBuilder = new MimeMessageBuilder(smtpServer.createAuthenticatedSession("jdoe", "somePassword")).
        from("source@smtp4j.local").
        to("target@smtp4j.local").
        subject("Message with multiple attachments").
        body("Hello,\nThis is some content.\n\nBye.");

    messageBuilder.send();

    List<SmtpMessage> received = smtpServer.readReceivedMessages();
    assertEquals(1, received.size());
}
```

It is possible to support multiple authentication schemes and users:
```java
try(SmtpServer smtpServer = new SmtpServerBuilder().
    withAuthenticator(PlainAuthenticationHandler.INSTANCE).
    withAuthenticator(LoginAuthenticationHandler.INSTANCE).
    withAuthenticator(CramMD5AuthenticationHandler.INSTANCE).
    withUser("jdoe", "somePassword").
    withUser("asmith", "otherPassword").
    withUser("mwindu", "customPasword").
    withPort(1025).
    start()) {
    //...
}
```

When at least one authentication scheme is declared, the `Session` created will contain
the following properties:

| Property | Value |
| -------- | ----- |
| `mail.smtp.auth` | `true` |
| `mail.smtp.sasl.enable` | `true` (only with `CRAM-MD5`) |

**Note**: The authentication scheme `CRAM-MD5` is [deprecated](https://en.wikipedia.org/wiki/CRAM-MD5) and should not be used in production.

### Connection listener

Once a `Socket` is connected, prior to any SMTP exchange the API will notify the listener, allowing
to easily implement a simple firewall. Simply throw an `IOException` to close the connection to
the remote host.

```java
SmtpServerBuilder builder = new SmtpServerBuilder();
builder.withConnectionListener((InetAddress remoteHost) -> {
    String host = remoteHost.getHostAddress();
    if(!host.equals("127.0.0.1")) { throw new IOException("connection refused from "+host); }
});

try(SmtpServer server = builder.start()) {
    //any connection from any other IP than localhost will be denied
}
```

### Multithreading

The builder allows you to specified an `ExecutorService` to use for thread handling.

```java
SmtpServerBuilder builder = new SmtpServerBuilder();
builder.withExecutorService(() -> Executors.newWorkStealingPool());

try(SmtpServer server = builder.start()) {
    //the executor service is created once the SMTP server is started and 
    //shut down when stopped
}
```

### Debugging Internals

It is very simple to enable debugging to see all the inputs/outputs of the underlying SMTP protocol.

```java
SmtpServer smtpServer = new SmtpServerBuilder().
    withDebugStream(System.err).
    start();
```

The output will be like this:

```
< 220 localhost smtp4j server ready
> EHLO galactus
< 250 smtp4j greets galactus
> MAIL FROM:<source@smtp4j.local>
< 250 OK
> RCPT TO:<target@smtp4j.local>
< 250 OK
> DATA
< 354 Start mail input; end with <CRLF>.<CRLF>
> Date: Thu, 3 Apr 2025 23:43:02 +0200 (CEST)
> From: source@smtp4j.local
> To: target@smtp4j.local
> Message-ID: <1428475041.1.1743716582473@galactus>
> Subject: Subject
> MIME-Version: 1.0
> Content-Type: multipart/mixed;
> boundary="----=_Part_0_238357312.1743716582458"
> 
> ------=_Part_0_238357312.1743716582458
> Content-Type: text/plain; charset=us-ascii
> Content-Transfer-Encoding: 7bit
> 
> Message
> ------=_Part_0_238357312.1743716582458--
> .
< 250 OK
> QUIT
< 221 goodbye
```

## Donate

This project is completely developed during my spare time.

Since I'm a big fan of cryptocurrencies and especially [Cardano](https://cardano.org) (ADA), you can send me
some coins at the address below (check it [here](https://cardanoscan.io/address/addr1q9sgms4vc038nq7hu4499yeszy0rsq3hjeu2k9wraksle8arg0n953hlsrtdzpfnxxw996l4t6qu5xsx8cmmakjcqhksaqpj66)):

```
addr1q9sgms4vc038nq7hu4499yeszy0rsq3hjeu2k9wraksle8arg0n953hlsrtdzpfnxxw996l4t6qu5xsx8cmmakjcqhksaqpj66
```
