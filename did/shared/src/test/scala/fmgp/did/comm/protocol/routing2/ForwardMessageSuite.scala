package fmgp.did.comm.protocol.routing2

import munit._
import zio.json._
import zio.json.ast.Json

import fmgp.did.comm._

/** didJVM/testOnly fmgp.did.comm.protocol.routing2.ForwardMessageSuite
  */
class ForwardMessageSuite extends FunSuite {

  val msg = EncryptedMessageExamples.obj_encryptedMessage_ECDHES_X25519_XC20P
  val id = MsgID()

  test("Build a ForwardMessage for an Example") {

    val fMsg1 = ForwardMessage.buildForwardMessage(id = id, next = msg.recipientsSubject.head, msg = msg)

    val fMsg2 = msg.toAttachmentJson match
      case Left(error) => fail(error)
      case Right(attachment) =>
        val plaintext = PlaintextMessageClass(
          `type` = ForwardMessage.piuri,
          id = id,
          to = Some(Set.empty),
          from = None,
          body = s"""{"next":"${msg.recipientsSubject.head}"}"""
            .fromJson[JSON_RFC7159]
            .getOrElse(JSON_RFC7159()),
          attachments = Some(Seq(attachment))
        )
        ForwardMessage.fromPlaintextMessage(plaintext)

    assertEquals(fMsg1, fMsg2)
    assertEquals(fMsg1.hashCode, fMsg2.hashCode)

    (fMsg1, fMsg2) match {
      case (Right(msg1), Right(msg2)) =>
        assertEquals(msg1.msg, msg2.msg)
        assertEquals(msg1.msg.hashCode, msg2.msg.hashCode)
      case (a, b) => fail("fMsg1 and fMsg2 MUST be Right")
    }

    // fMsg3 is Base64
    val fMsg3 = ForwardMessageBase64(
      id = id,
      to = Set.empty,
      from = None,
      next = msg.recipientsSubject.head,
      msg = msg,
    )
  }

  test("Encode Msg in ForwardMessageJson and in ForwardMessageBase64") {

    val fMsg1 = ForwardMessage.buildForwardMessage(id = id, next = msg.recipientsSubject.head, msg = msg)

    // fMsg3 is Base64
    val msg3 = ForwardMessageBase64(
      id = id,
      to = Set.empty,
      from = None,
      next = msg.recipientsSubject.head,
      msg = msg,
    )

    (fMsg1) match {
      case (Right(msg1)) =>
        assertEquals(msg1.msg, msg3.msg)
        assertEquals(msg1.msg.hashCode, msg3.msg.hashCode)
      case (a) => fail("fMsg1 MUST be Right")
    }

  }
}
