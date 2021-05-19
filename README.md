[![Maven](https://img.shields.io/maven-central/v/ch.astorm/smtp4j.svg)](https://search.maven.org/search?q=g:ch.astorm%20AND%20a:smtp4j)
[![Build](https://travis-ci.com/ctabin/smtp4j.svg?branch=master)](https://travis-ci.com/ctabin/smtp4j)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/ctabin/smtp4j.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/ctabin/smtp4j/context:java)

# smtp4j

Simple API to fake an SMTP server for Unit testing (and more).

## About this project

This API is inspired from [dumbster](https://github.com/kirviq/dumbster) with the following improvements:
- Dynamic port lookup feature
- Support of MIME messages with attachments
- Up-to-date dependencies
- Extended tests
- Numerous bugfixes

This API is compatible with the JDK 8 and higher.

## Installation (maven)

Use the following dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>ch.astorm</groupId>
    <artifactId>smtp4j</artifactId>
    <version>1.1</version>
</dependency>
```

## Quick Start Guide

Here is a quick example of the usage of this API that show an oversight of
how it can be used:

```java
/* SMTP server is started on port 1025 */
SmtpServerBuilder builder = new SmtpServerBuilder();
try(SmtpServer server = builder.withPort(1025).start()) {
    
    /* create and send an SMTP message */
    MimeMessageBuilder messageBuilder = new MimeMessageBuilder(server);
    messageBuilder.from("source@smtp4j.local").
                   to("target1@smtp4j.local", "John Doe <john@smtp4j.local>").
                   cc("target3@smtp4j.local").
                   subject("Hello, world !").
                   body("Hello\r\nGreetings from smtp4j !\r\n\r\nBye.");
    nessageBuilder.send();

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
      nessageBuilder.send();
      
      //process the received message
      //...
}
```

It is also possible to use this builder in a production application by using the
dedicated `Session` constructor:

```java
MimeMessageBuilder messageBuilder = new MimeMessageBuilder(session);
```
