package io.iohk.atala.mediator

import zio.*
import zio.http.*

object TraceIdMiddleware {

  private val NAME_TRACE_ID = "requestid"

  private def requestId(req: Request) = ZIO.logAnnotateScoped(
    req.headers
      .find(h => h.headerName.equalsIgnoreCase("x-request-id"))
      .toSet
      .map(h => LogAnnotation(NAME_TRACE_ID, h.renderedValue))
  )

  def addTraceId = {
    HandlerAspect.interceptHandler(
      Handler.fromFunctionZIO[Request] { request =>
        val effect = requestId(request)
        effect.map(_ => (request, ()))
      }
    )(Handler.identity)
  }

}
