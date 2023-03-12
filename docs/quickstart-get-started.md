# Getting Started with DIDComm Messaging in Scala

Here's a basic example of how to send a message using the library:

```scala mdoc
import zio._
import zio.json._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.comm.protocol.basicmessage2._

// Initialize a new message
val message = new BasicMessage(
  to = Set(TO("did:example:123")),
  content = "Hello, World!",
  from = Some(FROM("did:example:456")), 
)


message.toPlaintextMessage.toJsonPretty

// TODO Send the message
// didcomm.send(message).onComplete {
//   case Success(_) => println("Message sent!")
//   case Failure(err) => println(s"Error: $err")
// }
```


To receive messages, you can use the following code:

```scala mdoc
// TODO
// didcomm.onMessage { message =>
//   println(s"Received message from ${message.from}: ${message.body}")
// }
```

## Further Resources

The [library's documentation]
The [DIDComm V2 specification](https://identity.foundation/didcomm-messaging/spec/)
The [Decentralized Identity Foundation (DIF)][https://identity.foundation/]
