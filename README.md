# smtp4j

Simple API to fake an SMTP server for Unit testing (and more).

## About this project

This API is inspired from [dumbster](https://github.com/rjo1970/dumbster) with the following improvements:
- Dynamic port lookup feature
- Support of MIME messages with attachments
- Up-to-date dependencies
- Extended tests
- JDK8+ support

## Installation (maven)

Use the following dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>ch.astorm</groupId>
    <artifactId>smtp4j</artifactId>
    <version>1.0</version>
</dependency>
```

## Quick Start Guide

Here is a quick example of the usage of this API that show an oversight of
how it can be used:

```java
/* SMTP server is started on port 1025 */
SmtpServerBuilder builder = new SmtpServerBuilder();
try(SmtpServer server = builder.withPort(1025).start()) {
    
    /* configure the SMTP client to send messages to localhost:1025 */
    Properties smtpProps = new Properties();
    smtpProps.setProperty("mail.smtp.host", "localhost");
    smtpProps.setProperty("mail.smtp.port", "1025");
    
    /* create an SMTP message */
    Session session = Session.getDefaultInstance(smtpProps);
    MimeMessage msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress("noreply@local.host"));
    msg.addRecipient(RecipientType.TO, new InternetAddress("cedric@smtp4j.com"));
    msg.setSubject("My first message !", StandardCharsets.UTF_8.name());
    msg.setText("Hello,\r\n\r\nGreetings from smtp4j !\r\n\r\nBy.", StandardCharsets.UTF_8.name());

    /* send the message */
    Transport.send(msg);

    /* retrieve the sent message from smtp4j */
    List<SmtpMessage> messages = server.getReceivedMessages();
    assertEquals(1, messages.size());
    
    /* analyze the content of the message */
    SmtpMessage receivedMessage = messages.get(0);
    String from = receivedMessage.getFrom();
    String subject = receivedMessage.getSubject();
    String body = receivedMessage.getBody();
    Date sentDate = receivedMessage.getSentDate();
    List<String> recipientsTo = receivedMessage.getRecipients(RecipientType.TO);
    List<SmtpAttachment> attachments = receivedMessage.getAttachments();
    
    /* clear the messages for further tests, if necessary */
    server.clearReceivedMessages();
}
```

## Usage

Here are some usages about specific parts of the API. For more examples,
look in the [tests](src/test/java/ch/astorm/smtp4j).

Basically, it is recommanded to always use the [SmtpServerBuilder](src/main/java/ch/astorm/smtp4j/SmtpServerBuilder.java)
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

### Received messages

The received messages can be accessed directly through the `SmtpServer`:

```java
List<SmtpMessage> receivedMessages = smtpServer.getReceivedMessages();
```

Once the messages have been processed, it is possible to clear them:

```java
smtpServer.clearReceivedMessages();
```

#### Message handling

By default, once a `SmtpMessage` has been received, it will be stored in an internal
[SmtpMessageStorage](src/main/java/ch/astorm/smtp4j/core/SmtpMessageStorage.java) instance,
which can be directly accessed like this:

```java
SmtpMessageHandler messageHandler = smtpServer.getMessageHandler();
SmtpMessageStorage messageStorage = (SmtpMessageStorage)messageHandler;
```

It is possible to override this default behavior with your custom handler with the
following piece of code:

```java
SmtpMessageHandler myCustomHandler = smtpMessage -> System.out.println("Message received from: "+smtpMessage.getFrom());

SmtpServerBuilder builder = new SmtpServerBuilder();
try(SmtpServer server = builder.withMessageHandler(myCustomHandler).start()) {
    //...
}
```

If you want then to reset the default behavior:

```java
//reset the internal message handler
smtpServer.setMessageHandler(null);
```

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

It is also possible retrieve some data directly issued from the SMTP exchange
with the server. Those data might differ (even be missing) from the underlying
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
