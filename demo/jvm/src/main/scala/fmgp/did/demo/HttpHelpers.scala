package fmgp.did.demo

import zio._
import zio.http._

extension [R, Err](app: HttpApp[R, Err])
  def annotateLogs: HttpApp[R, Err] =
    app @@ new HttpAppMiddleware.Simple[Any, Nothing] { self1 =>
      override def apply[R, Err](
          http: Http[R, Err, Request, Response],
      )(implicit trace: Trace): Http[R, Err, Request, Response] =
        http.mapZIO { request =>
          val headers = request.headers.toSeq.map(e => (e.key.toString.toLowerCase, e.value)).toMap
          inline def composeAnnotate(
              inline headerName: String,
              inline logName: String,
              inline run: ZIO[R, Err, Response]
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
                "user-agent",
                "user-agent",
                composeAnnotate(
                  "host",
                  "host",
                  ZIO.succeed(request)
                )
              )
            )
          )

        }
    }
