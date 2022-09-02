package fmgp.did

import zio._
import fmgp.crypto._

/** Agent have is a DID
  *   - has keys
  *   - has encryption preferences
  *   - has a resolver
  */
trait Agent {
  def id: DID
  def keys: Seq[OKP_EC_Key]
}
