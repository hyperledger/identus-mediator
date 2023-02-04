package fmgp.did.comm

import zio._
import fmgp.crypto._
import fmgp.did.comm._

/** Hide implementation details to improve the API */
extension (c: Operations.type)
  def layerDefault: ULayer[Operations] =
    ZLayer.succeed(OperationsImp(CryptoOperationsImp))
