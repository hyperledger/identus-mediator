package io.iohk.atala.mediator.utils

import zio.*

type SocketID = String
case class MyChannel(id: SocketID, socketOutHub: Hub[String])
