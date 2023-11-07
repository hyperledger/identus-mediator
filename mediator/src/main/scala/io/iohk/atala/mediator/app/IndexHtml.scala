package io.iohk.atala.mediator.app

import zio.http._
import fmgp.did.DID

object IndexHtml {
  def html(identity: DID) =
    Response(
      status = Status.Ok,
      headers = Headers(Header.ContentType(MediaType.text.html).untyped),
      body = Body.fromString(
        s"""<!DOCTYPE html>
           |<html>
           |<head>
           |  <meta charset="UTF-8">
           |  <title>IOHK Mediator</title>
           |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
           |  <meta name="did" content="${identity.did}">
           |  <link href="https://fonts.googleapis.com/css?family=Roboto:300,400,500" rel="stylesheet">
           |  <link href="https://unpkg.com/material-components-web@latest/dist/material-components-web.min.css" rel="stylesheet">
           |  <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">
           |  <script>
           |    var callback = function () {
           |      alert('A callback was triggered');
           |    }
           |  </script>
           |  <!-- My APP -->
           |  <script type="text/javascript" src="public/webapp-fastopt-bundle.js"></script>
           |</head>
           |
           |<body style="margin:0;">
           |  <div id="app-container"></div>
           |</body>
           |
           |</html>""".stripMargin
      ),
    )
}
