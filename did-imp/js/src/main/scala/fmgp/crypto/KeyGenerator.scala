package fmgp.crypto

import zio._
import zio.json._
import scala.util.Try

import fmgp.crypto.error._
import fmgp.crypto.OKPPrivateKey
import typings.jose.mod.generateKeyPair
import typings.jose.generateKeyPairMod.GenerateKeyPairResult
import scala.scalajs.js.JSON

// FIXME
// TODO test this on a real browser!
object KeyGenerator {

// await store.generate("RSA",2048,{alg:"RS256", key_ops:["sign", "decrypt", "unwrap"]});
// lkey = (await store.get());
// var key = lkey.toJSON(); //get public key to exchange
// key.use = "sig";
// key.key_ops=["encrypt","verify", "wrap"];

// var pubKey = await jose.JWK.asKey(key);
// key = null;

  def newX25519: Either[FailToGenerateKey, OKPPrivateKey] = Left(
    FailToGenerateKey(new RuntimeException("TODO FIXME"))
  ) // FIXME
  def newEd25519: Either[FailToGenerateKey, OKPPrivateKey] = Left(
    FailToGenerateKey(new RuntimeException("TODO FIXME"))
  ) // FIXME

  // https://github.com/panva/jose/blob/main/docs/functions/key_generate_key_pair.generateKeyPair.md
  def makeX25519: IO[FailToGenerateKey, OKPPrivateKey] = {
    for {
      pair <- ZIO
        // .fromPromiseJS(generateKeyPair("PS256")) //PS384
        .fromPromiseJS(generateKeyPair("ES256K"))
        .catchAll { case ex =>
          ZIO.fail(FailToGenerateKey(ex))
        // Like scala.scalajs.js.JavaScriptException: JOSENotSupported: Invalid or unsupported JWK "alg" (Algorithm) Parameter value
        }
      publicKey = pair
      privateKey = pair
      _ = println(JSON.stringify(pair))
      _ = println(publicKey.toString())
      _ = println(publicKey)
      _ = println(privateKey)
      _ = println()
      _ <- Console
        .printLine(publicKey)
        .catchAll { case ex =>
          ZIO.fail(FailToGenerateKey(ex))
        }
      ret: OKPPrivateKey <- ZIO.fail(FailToGenerateKey(new RuntimeException("TODO FIXME")))
    } yield (ret)
  }

  def makeEd25519: IO[FailToGenerateKey, OKPPrivateKey] =
    ZIO.fail(FailToGenerateKey(new RuntimeException("TODO FIXME")))
}
