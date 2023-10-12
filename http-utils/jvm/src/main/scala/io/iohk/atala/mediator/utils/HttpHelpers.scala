package io.iohk.atala.mediator.utils

import zio.*
import zio.http.*
import java.util.concurrent.TimeUnit

object MiddlewareUtils {

  final def serverTime: RequestHandlerMiddleware[Nothing, Any, Nothing, Any] = HttpAppMiddleware.patchZIO(_ =>
    for {
      currentMilliseconds <- Clock.currentTime(TimeUnit.MILLISECONDS)
      withHeader = Response.Patch.addHeader("X-Time", currentMilliseconds.toString)
    } yield withHeader,
  )

  final def annotateHeaders: RequestHandlerMiddleware[Nothing, Any, Nothing, Any] =
    new RequestHandlerMiddleware.Simple[Any, Nothing] {
      override def apply[R1 <: Any, Err1 >: Nothing](
          handler: Handler[R1, Err1, Request, Response],
      )(implicit trace: Trace): Handler[R1, Err1, Request, Response] =
        Handler.fromFunctionZIO { (request: Request) =>

          def annotations = request.headers.toSet.flatMap(h =>
            h.headerName.toLowerCase() match {
              case "fly-client-ip"  => Some(LogAnnotation("client-ip", h.renderedValue))
              case "fly-request-id" => Some(LogAnnotation("fly-request-id", h.renderedValue))
              case "x-request-id"   => Some(LogAnnotation("x-request-id", h.renderedValue))
              case "user-agent"     => Some(LogAnnotation("user-agent", h.renderedValue))
              case "host"           => Some(LogAnnotation("host", h.renderedValue))
              case _                => None
            }
          )

          val requestHandler = handler
            .runZIO(request)
            .sandbox
            .exit
            .timed
            .tap {
              case (duration, Exit.Success(response)) =>
                ZIO.log(s"${response.status.code} ${request.method} ${request.url.encode} ${duration.toMillis}ms")
              case (duration, Exit.Failure(cause)) =>
                ZIO.log(s"Failed ${request.method} ${request.url.encode} ${duration.toMillis}ms: " + cause.prettyPrint)
            }
            .flatMap(_._2)
            .unsandbox

          ZIO.logAnnotate(annotations)(requestHandler)
        }
    }

}
