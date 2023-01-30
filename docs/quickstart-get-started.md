# Getting Started with DIDComm Messaging in Scala

Here's a basic example of how to send a message using the library:

```scala mdoc
import zio._; zio.json._ 
import fmgp.did._, fmgp.did.com._

// Initialize a new message
val message = new Message(to = "did:example:123", from = "did:example:456", body = "Hello, World!")

// Send the message
didcomm.send(message).onComplete {
  case Success(_) => println("Message sent!")
  case Failure(err) => println(s"Error: $err")
}
```


To receive messages, you can use the following code:

```scala mdoc
didcomm.onMessage { message =>
  println(s"Received message from ${message.from}: ${message.body}")
}
```

## Further Resources

The [library's documentation]()
The [DIDComm V2 specification](https://identity.foundation/didcomm-messaging/spec/)
The [Decentralized Identity Foundation (DIF)][https://identity.foundation/]
