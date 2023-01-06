package fmgp.did.comm.protocol.routing2

import munit._
import zio.json._
import zio.json.ast.Json

import fmgp.did.comm._

/** didJVM/testOnly fmgp.did.comm.protocol.routing2.ForwardMessageSuite
  */
class ForwardMessageSuite extends FunSuite {

  test("Build a ForwardMessage for an Example") {
    val msg = EncryptedMessageExamples.obj_encryptedMessage_ECDHES_X25519_XC20P

    val id = MsgID()

    val fMsg1 = ForwardMessage.buildForwardMessage(id = id, next = msg.recipientsSubject.head, msg = msg)

    val fMsg2 = msg.toAttachmentJson match
      case Left(error) => fail(error)
      case Right(attachment) =>
        Right(ForwardMessage(id = id, next = msg.recipientsSubject.head, attachments = Seq(attachment)))

    assertEquals(fMsg1, fMsg2)
    assertEquals(fMsg1.hashCode, fMsg2.hashCode)
  }
}
