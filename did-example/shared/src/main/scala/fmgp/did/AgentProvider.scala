package fmgp.did

import zio._
import fmgp.crypto._
import fmgp.did.Agent
import fmgp.did.resolver.peer.DIDPeer2
import fmgp.did.resolver.peer.DIDPeerServiceEncoded
import fmgp.did.resolver.peer.DIDPeer
import fmgp.did.resolver.hardcode.HardcodeResolver

object AgentProvider {

  /** https://mermaid.live/edit#pako:eNpVkMFqwzAMhl_F6Ny-gA-FbVmht8LKLnEPaqwsooltFDswSt-9cpvDdpD8S__3G-MbdNETWPgRTIM5NS4Y8zZyR2a73Zkj5lbLuZAkLiTnar_Hy9NscKG_82HB0NamOM9zWfGPAWXk132fC7VaCpCGZy8xpRUz1XxCe8Fw_b_65i5HaV-HpvUp3POa3OOFYxWePWxgIplQlYVbXTrIA03kwKr01GMZswMX7opiyfHrN3RgsxTaQEkeMzWM-hsT2B7Hme4PPpxgwQ
    */
  def usersGraph = s"""
  |graph LR
  |  subgraph alice.did.fmgp.app
  |    Alice --> Pat[Pat prover]
  |  end
  |  subgraph bob.did.fmgp.app
  |    Bob --> Dave
  |    Bob --> Ivan[Ivan issuer]
  |  end
  |  subgraph charlie.did.fmgp.app
  |    Charlie --> Eve[Eve eavesdropper]
  |    Eve --> Frank
  |    Eve --> Victor[Victor verifier]
  |  end
  |  subgraph localhost:8080
  |    local
  |  end
  |  subgraph fabio.did.fmgp.app
  |    Fabio
  |  end
  |  subgraph did.fmgp.app
  |  end
  |
  |  click Alice "#/resolver/${alice.id.did}" "Link to Alice DID Document"
  |  click Bob "#/resolver/${bob.id.did}" "Link to Bob DID Document"
  |  click Charlie "#/resolver/${charlie.id.did}" "Link to Charlie DID Document"
  |  click Pat "#/resolver/${pat.id.did}" "Link to Pat DID Document"
  |  click Dave "#/resolver/${dave.id.did}" "Link to Dave DID Document"
  |  click Ivan "#/resolver/${ivan.id.did}" "Link to Ivan DID Document"
  |  click Eve "#/resolver/${eve.id.did}" "Link to Eve DID Document"
  |  click Frank "#/resolver/${frank.id.did}" "Link to Frank DID Document"
  |  click Victor "#/resolver/${victor.id.did}" "Link to Victor DID Document"
  |  click local "#/resolver/${local.id.did}" "Link to local DID Document"
  |""".stripMargin

  def allAgents: Map[String, Agent] = Map(
    "local" -> local,
    "alice" -> alice,
    "bob" -> bob,
    "charlie" -> charlie,
    "dave" -> dave,
    "eve" -> eve,
    "frank" -> frank,
    "ivan" -> ivan,
    "pat" -> pat,
    "victor" -> victor,
    "exampleAlice" -> exampleAlice,
    "exampleBob" -> exampleBob,
  )

  def allIdentities: Map[String, DID] =
    allAgents.view.mapValues(_.id).toMap ++
      DIDPeer
        .fromDID(
          DIDSubject(
            "did:peer:2.Ez6LSms555YhFthn1WV8ciDBpZm86hK9tp83WojJUmxPGk1hZ.Vz6MkmdBjMyB4TS5UbbQw54szm8yvMMf1ftGV2sQVYAxaeWhE.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwczovL21lZGlhdG9yLnJvb3RzaWQuY2xvdWQiLCJhIjpbImRpZGNvbW0vdjIiXX0"
          ).toDID
        )
        .toOption
        .map("rootsid" -> _) // https://mediator.rootsid.cloud/oob_url
        .toMap

  private def aliceURL = s"https://alice.did.fmgp.app/"
  private def bobURL = s"https://bob.did.fmgp.app/"
  private def charlieURL = s"https://charlie.did.fmgp.app/"

  private def keyAgreement(d: String, x: String) =
    OKPPrivateKey(kty = KTY.OKP, crv = Curve.X25519, d = d, x = x, kid = None)
  private def keyAuthentication(d: String, x: String) =
    OKPPrivateKey(kty = KTY.OKP, crv = Curve.Ed25519, d = d, x = x, kid = None)

  // did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y.Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd
  val alice = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("Z6D8LduZgZ6LnrOHPrMTS6uU2u5Btsrk1SGs4fn8M7c", "Sr4SkIskjN_VdKTn0zkjYbhGTWArdUNE4j_DmUpnQGw"),
      keyAuthentication("INXCnxFEl0atLIIQYruHzGd5sUivMRyQOzu87qVerug", "MBjnXZxkMcoQVVL21hahWAw43RuAG-i64ipbeKKqwoA")
    ),
    Seq(DIDPeerServiceEncoded(s = aliceURL))
  )
  // did:peer:2.Ez6LSkGy3e2z54uP4U9HyXJXRpaF2ytsnTuVgh6SNNmCyGZQZ.Vz6Mkjdwvf9hWc6ibZndW9B97si92DSk9hWAhGYBgP9kUFk8Z
  val bob = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("H5wHQcecUqobAMT3RiNsAaYaFXIfTLCNhWAYXgTYv7E", "f8ce_zxdhIEy76JE21XpVDviRtR2amXaZ6NjYyIPjg4"),
      keyAuthentication("LyMSyr_usdn3pHZc00IbJaS2RcvF4OcJTJIB2Vw6dLQ", "TQdV8Wduyz3OylN3YbyHR0R-aynF3C1tmvHAgl6b34I")
    ),
    Seq(DIDPeerServiceEncoded(s = bobURL))
  )
  // did:peer:2.Ez6LSbj1AHPALSc6v6jMzozr4HfE3siavLgjZ8XrriNTSWdkW.Vz6MkmL4nXx4qze1UU3hAcAVghn5WVZj2vbt2w9vpAWN85ZAS
  val charlie = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("J7aAYF3EDfKpfK392bgPV-bC2vaceq0jpMnfjM8agoQ", "ALluQw8hX9lN3G-vKEsGHJdteEMx7IN8qrNu3Z-o_wM"),
      keyAuthentication("dJ-k49IrQ3CQmrkrpHZvWYIdlMkcc6-hnQvrCa8hB1Q", "ZioC9PEG6LJA_Yf1sDTwQgTkKpK-LMWYYff0KUfeVZM")
    ),
    Seq(DIDPeerServiceEncoded(s = charlieURL))
  )
  // did:peer:2.Ez6LSoJXKbJuq1FvfMdCm3jnbTV4PLAN4qAs8sEEnZn6gNiBD.Vz6MkmAG1iP6YaTHzmU2n6wfkCuMep8swWEUdjwzmVH5EewjW)
  val pat = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("Z8CsybA4UuLWeSPdteHvTbpx1QVOhX-pMXsEsM1c74Y", "rMBBlYU8xMpzuwGp8Jff9z-bkepa2AwIdmZydrzKqCw"),
      keyAuthentication("6VvWEGxOv15e9EPgb1zXq7pnrbSMN9ediBulijUxvPA", "Y6bl7Gs7LqYLlt6bAjPzE8ywpc2xtKtiv8irsFS5f1E")
    ),
    Seq(DIDPeerServiceEncoded(s = alice.id.did))
  )
  // did:peer:2.Ez6LShTw9dYiKEixwH3vAmSufL7gb1Rvp2YGoy4VHYUWxpjmh.Vz6MknK57qz1oyQFdsDm9L29CNBJzqESqe5eTPYXTsg1yVJKw
  val dave = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("p2dYLzyj__m5gn6Fk4SQnmhbOar1QSff-IO6MBZlf7w", "VgNN-ldBGnukky38qz2G0lMLPF9UWH9NzEEcZM84ozA"),
      keyAuthentication("o7ZwF-S12lUtXrZl6B9GsrYo-sDpDdrTWz8MPsc-OXE", "dMR-f2jlNpk9IHJpEqbz_3GwU1Iby4ZCun-2s4Z3bk4")
    ),
    Seq(DIDPeerServiceEncoded(s = bob.id.did))
  )
  // did:peer:2.Ez6LSpFPZ4YmSHjEvSb2DsDdNJAPNhPWP48RowvpwvyKZRkjp.Vz6MkrpKnrHBvLREgP9VT4KZkEzdtjXxT9w8FMkZq2tuUjLDr
  val ivan = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("_yfk3UXYu6Wt1LxqbqI5ST0JdDC6aQS5TV-8CLb6u4s", "us5q6uu-DUC_49Bh04cZMOScKewx-Xg8kVlF7KSHpD8"),
      keyAuthentication("f31O53ZdLyF73RmuJTKqe1TiI0mXzQHiqdBquBj1wVg", "t7Gs8OdVn3eyZ21QB32qYPxK_vqAJ1DfaWAoYwEKUZU")
    ),
    Seq(DIDPeerServiceEncoded(s = bob.id.did))
  )
  // did:peer:2.Ez6LSi65Z5Xxq3Qyksu3Fy6gawopQM9hN5yqvcsKQQh8aqAZH.Vz6MkfoXdwNKyAVfz6QtmLkow4J5v9es21FsbocZwWbNaJzHs
  val eve = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("T0ra1wQ4FizXQtchN03lt3_DvKdd-_xVTtM39Z9Yl7I", "X0W3atVLGH3XWf6X9nRtmM0NYWzf7joyi-YEnKMkUgQ"),
      keyAuthentication("MIbKvsxw658nBM2SAuQBq3iRdbfTlqXuPLdQ23XtPVs", "FAzu6ZMsBrwGklCFBO3fAHNpDNa-IpOd_zuedLDfWa4")
    ),
    Seq(DIDPeerServiceEncoded(s = charlie.id.did))
  )
  // did:peer:2.Ez6LSdmPx5SxWD6XGmZk5EcYZD5EjB1yCgZAXzvuyabmNqYVz.Vz6MkhE8eyVZsZJjhk8rNXox5yiNywkNmzcMQcfAn5acg5Wcm
  val frank = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("_9NL5wwqOyFKGv4ikD-hMQYzqoEchXx5LXtGnM_F6Zo", "Hw28ApUI4byjBpyjTr2ALBPT4u0dBG1v7mlcGO7C2j0"),
      keyAuthentication("RMFyv5f_IoT5ikNewuzDUU1CBrdoaqfkvsRA2ywa2ys", "KTYniMnFo_8KLG3BP3Jg8ISEaUzKXtBgf9eZXKrfT14")
    ),
    Seq(DIDPeerServiceEncoded(s = eve.id.did))
  )

  // did:peer:2.Ez6LSi4HpG1Y3MQqRVJvMBbJBFKP329g6k8t1qC4GVxNsWNJB.Vz6MksAbrQeW9NLum9QTHMu2JHRYVM6LpLxoE8y39f8fYJNLr
  val victor = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("t0PNwt5UC3g-sblXAaJFKlhwxM3U-cqvFLvSxvRZqZM", "XtBrU4fKSFun-U2AkYJ3zs_X57bZq2vlGV7ZBXzDCGA"),
      keyAuthentication("g0wE5tKUba3wVvtbHwOtpG-XPfX14t2rXvclDw1uCuc", "vONzzXnYK4KC_j_d4FMflqhRjFsI7giAeCGq8j-1vos")
    ),
    Seq(DIDPeerServiceEncoded(s = eve.id.did))
  )

  // did:peer:2.Ez6LSm4qrZKu2svvVCMNGUwXG9FZRdRqrfU2AqyABFdgJ8xzV.Vz6Mks42Y68Na7B8xgV4RKedZGqXrKNp7ryymv3nDEnyZXBFY
  val local = DIDPeer2.makeAgent(
    Seq(
      keyAgreement("d5NNzHVrh13uEYd_IiEpW9xJPdjb60j-06AHcCdXT6c", "i4e1FMEyCC3uqBZvRH_yT2_soyvrhYxp6Mi33D4i-Eo"),
      keyAuthentication("mnMNIGXhYLiIEzk4KjCewN5QTtEHgSd_V5dAvpy1-_M", "uzRMEeJho3oGJUiJWi_YvVwTlUrDpJ4Z2LZcqmseZCM")
    ),
    Seq(DIDPeerServiceEncoded(s = "http://localhost:8080"))
  )

  val exampleAlice = new Agent {
    override def id: DID = DidExample.senderDIDDocument.id
    override def keys: Seq[fmgp.crypto.PrivateKey] = DidExample.senderSecrets.keys.toSeq
  }

  val exampleBob = new Agent {
    override def id: DID = DidExample.recipientDIDDocument.id
    override def keys: Seq[fmgp.crypto.PrivateKey] = DidExample.recipientSecrets.keys.toSeq
  }
}
