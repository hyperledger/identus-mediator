package io.iohk.atala.mediator

import zio._
import fmgp.did.framework.TransportFactory
import zio.http._
import fmgp.did.framework.TransportFactoryImp

object MediatorTransportManagerUtil {

  // utility
  def layerTest: ZLayer[Any, Nothing, Ref[MediatorTransportManager]] =
    Scope.default >>> (Client.default >>> TransportFactoryImp.layer).orDie >>>
      ZLayer.fromZIO(MediatorTransportManager.make)
}
