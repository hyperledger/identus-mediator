package fmgp.did.demo

import zio._
import zio.http._

// object HttpHelpers {
extension [R, Err](app: HttpApp[R, Err])
  def annotateLogs: HttpApp[R, Err] =
    app @@ new HandlerAspect[R, Err, Request, Response, Request, Response] { self1 =>
      override def apply[R1 <: Any, Err1 >: Nothing](handler: Handler[R1, Err1, Request, Response])(implicit
          trace: Trace,
      ) = Handler.FromFunctionZIO(()).apply { r =>
        val headers = r.headers.toSeq.map(e => (e.key.toString.toLowerCase, e.value)).toMap
        inline def composeAnnotate(
            inline headerName: String,
            inline logName: String,
            inline run: ZIO[R1, Err1, Response]
        ) = headers.get(headerName) match
          case Some(value) => ZIO.logAnnotate(logName, value.toString) { run }
          case None        => run
        composeAnnotate(
          "fly-client-ip",
          "client-ip",
          composeAnnotate(
            "request-id",
            "fly-request-id",
            composeAnnotate(
              "host",
              "host",
              handler.runZIO(r)
            )
          )
        )
      }
    }.toMiddleware
// }
