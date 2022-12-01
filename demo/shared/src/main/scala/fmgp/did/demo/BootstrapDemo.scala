package fmgp.did.demo

import zio._
import zio.json._

import fmgp.did._
import fmgp.did.resolver.peer.DIDPeer
import fmgp.crypto.OKPPrivateKey
import fmgp.crypto.PrivateKey

object BootstrapDemo extends ExDID0 {

  val agent: Agent = new Agent {
    def id: DID = did
    def keys: Seq[PrivateKey] = Seq(
      keyAuthentication.copy(kid = Some(did.string + "#6MkhNpHBCUgBgkCbiM4zMjrbfgGowwEuEchmzf6J5W3av8E")),
      keyAgreement.copy(kid = Some(did.string + "#6LSpou63sBDB4FGpbVM23bECgZnkMHj6hGmA3PgQByR9fs4")),
    )
  }
  def agentLayer: ULayer[Agent] = ZLayer.succeed(agent)

}

trait ExDID0 { // https://localhost:9090/
  val did = DIDPeer(
    DIDSubject(
      "did:peer:2.Ez6LSpou63sBDB4FGpbVM23bECgZnkMHj6hGmA3PgQByR9fs4.Vz6MkhNpHBCUgBgkCbiM4zMjrbfgGowwEuEchmzf6J5W3av8E.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo5MDkwLyIsInIiOltdLCJhIjpbImRpZGNvbW0vdjIiXX0"
    )
  )
  val keyAgreement =
    """{"kty":"OKP","d":"b6JxUFsuwKPkOUAvU4FXEYzC7oKhzwNOM1aWyfvHP7k","crv":"X25519","x":"wyJl5uFCb4OXE_KuRePrM92z6aPfk8PXBHIpg1rG528"}"""
      .fromJson[OKPPrivateKey]
      .toOption
      .get // TODO get
  val keyAuthentication =
    """{"kty":"OKP","d":"i6gfpmHFrSHbwAa7gI-bnOL0gMyePZ6Pe1xN-TDPMO4","crv":"Ed25519","x":"K2-apfExODENs4ZGlZ2xh8DSq4vkCqG3fgS3Ofwslv8"}"""
      .fromJson[OKPPrivateKey]
      .toOption
      .get // TODO get
}

object AgentEX1 extends ExDID1 {
  val agent: Agent = new Agent {
    def id: DID = did
    def keys: Seq[PrivateKey] = Seq(
      keyAuthentication.copy(kid = Some(did.string + "#6MkeTY54nNujTFzUQH6DuERrqXjwKe1dbKDa7nzFS8GNPq1"))
    )
  }
  def layer: ULayer[AgentEX1.type] = ZLayer.succeed(this)
}

trait ExDID1 { // https://localhost:9091/
  val did = DIDPeer(
    DIDSubject(
      "did:peer:2.Ez6LSdeknAoZHfA2QWsvbRbh5UZ1NPHP38NjBDdHrrhACJUgh.Vz6MkeTY54nNujTFzUQH6DuERrqXjwKe1dbKDa7nzFS8GNPq1.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTA5MS8iLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19"
    )
  )
  val keyAgreement =
    """{"kty":"OKP","d":"ZwV0ySU6tXxQlAzw8ji79H8bdeve6vGzruG_SYJen5Q","crv":"X25519","x":"HVo4aqtv6pL660P_4yBWQtVZLQrw0kqhzzKPyaI1-EI"}"""
      .fromJson[OKPPrivateKey]
      .toOption
      .get // TODO get
  val keyAuthentication =
    """{"kty":"OKP","d":"4OQpka1tpCMhz-oNpeobkmDg2NiD4JIisH5WBB1etMM","crv":"Ed25519","x":"ABIeuNr5_0rfwXGC2dgCd8Ab8AqfcB3DGrDwk70PJtA"}"""
      .fromJson[OKPPrivateKey]
      .toOption
      .get // TODO get
}

object AgentEX2 extends ExDID2 {
  val agent: Agent = new Agent {
    def id: DID = did
    def keys: Seq[PrivateKey] = Seq(
      keyAuthentication.copy(kid = Some(did.string + "#6MksEtp5uusk11aUuwRHzdwfTxJBUaKaUVVXwFSVsmUkxKF"))
    )
  }
  def layer: ULayer[AgentEX2.type] = ZLayer.succeed(this)
}

trait ExDID2 { // https://localhost:9092/
  val did = DIDPeer(
    DIDSubject(
      "did:peer:2.Ez6LSq12DePnP5rSzuuy2HDNyVshdraAbKzywSBq6KweFZ3WH.Vz6MksEtp5uusk11aUuwRHzdwfTxJBUaKaUVVXwFSVsmUkxKF.SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6OTA5My8iLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19"
    )
  )
  val keyAgreement =
    """{"kty":"OKP","d":"9yAs1ddRaUq4d7_HfLw2VSj1oW2kirb2wALmPXrRuZA","crv":"X25519","x":"xfvZlkAnuNpssHOR2As4kUJ8zEPbowOIU5VbhBsYoGo"}"""
      .fromJson[OKPPrivateKey]
      .toOption
      .get // TODO get
  val keyAuthentication =
    """{"kty":"OKP","d":"-yjzvLY5dhFEuIsQcebEejbLbl3b8ICR7b2y2_HqFns","crv":"Ed25519","x":"vfzzx6IIWdBI7J4eEPHuxaXGErhH3QXnRSQd0d_yn0Y"}"""
      .fromJson[OKPPrivateKey]
      .toOption
      .get // TODO get
}
