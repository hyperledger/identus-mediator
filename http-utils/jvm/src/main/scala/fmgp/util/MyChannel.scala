package fmgp.util

import zio._

type SocketID = String
case class MyChannel(id: SocketID, socketOutHub: Hub[String])
