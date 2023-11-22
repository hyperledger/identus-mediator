package io.iohk.atala.mediator

import zio._
import zio.json._
import zio.stream._
import zio.http._
import zio.http.Header.{AccessControlAllowOrigin, AccessControlAllowMethods}

import fmgp.crypto._
import fmgp.crypto.error._
import fmgp.did._
import fmgp.did.comm._
import fmgp.did.framework._
import fmgp.did.method.peer.DidPeerResolver
import fmgp.did.method.peer.DIDPeer.AgentDIDPeer

object DIDCommRoutes {

  def app: HttpApp[Operator & Operations & Resolver] = routes.toHttpApp

  def routes: Routes[Operator & Operations & Resolver, Nothing] = Routes(
    Method.GET / "ws" -> handler { (req: Request) =>
      for {
        annotationMap <- ZIO.logAnnotations.map(_.map(e => LogAnnotation(e._1, e._2)).toSeq)
        webSocketApp = TransportWSImp.createWebSocketAppWithOperator(annotationMap)
        ret <- webSocketApp.toResponse
      } yield (ret)
    },
    Method.POST / trailing -> handler { (req: Request) =>
      val SignedTyp = MediaTypes.SIGNED.typ
      val EncryptedTyp = MediaTypes.ENCRYPTED.typ
      // FIXME after https://github.com/zio/zio-http/issues/2416
      // .header(Header.ContentType)
      // .exists { h =>
      //   h.mediaType.mainType == MediaTypes.mainType &&
      //   (h.mediaType.subType == MediaTypes.SIGNED.subType || h.mediaType.subType == MediaTypes.ENCRYPTED.subType)
      req.headers.get("content-type") match
        case Some(`SignedTyp`) | Some(`EncryptedTyp`) =>
          (for {
            data <- req.body.asString
            msg <- data.fromJson[Message] match
              case Left(value) => ZIO.fail(Response.badRequest(s"Fail to parse DID Comm Message because: $value"))
              case Right(pMsg: PlaintextMessage) => ZIO.fail(Response.badRequest("Message must not be in Plaintext"))
              case Right(sMsg: SignedMessage)    => ZIO.succeed(sMsg)
              case Right(eMsg: EncryptedMessage) => ZIO.succeed(eMsg)
            inboundQueue <- Queue.bounded[SignedMessage | EncryptedMessage](1)
            outboundQueue <- Queue.bounded[SignedMessage | EncryptedMessage](1)
            transport = new TransportDIDComm[Any] {
              def id: TransportID = TransportID.http(req.headers.get("request_id"))
              def inbound: ZStream[Any, Transport.InErr, SignedMessage | EncryptedMessage] =
                ZStream.fromQueue(inboundQueue)
              def outbound: ZSink[Any, Transport.OutErr, SignedMessage | EncryptedMessage, Nothing, Unit] =
                ZSink.fromQueue(outboundQueue)

              // TODO def close = inboundQueue.shutdown <&> outboundQueue.shutdown
            }
            operator <- ZIO.service[Operator]
            fiber <- operator.receiveTransport(transport).fork
            _ <- inboundQueue.offer(msg)
            ret <- outboundQueue.take
              .timeout(3.seconds)
              .tap(e => ZIO.logWarning("Request Timeout").when(e.isEmpty))
              .map {
                case None => Response.status(Status.Accepted)
                case Some(msg: SignedMessage) =>
                  Response(Status.Ok, Headers(MediaTypes.SIGNED.asContentType), Body.fromCharSequence(msg.toJson))
                case Some(msg: EncryptedMessage) =>
                  Response(Status.Ok, Headers(MediaTypes.ENCRYPTED.asContentType), Body.fromCharSequence(msg.toJson))
              }
            shutdown <- inboundQueue.shutdown <&> outboundQueue.shutdown
            _ <- fiber.join
          } yield ret)
            .tapErrorCause(ZIO.logErrorCause("Error", _))
            .catchAllCause(cause => ZIO.succeed(Response.fromCause(cause)))
        case Some(_) | None => ZIO.succeed(Response.badRequest(s"The content-type must be $SignedTyp or $EncryptedTyp"))

    },
  )
}
