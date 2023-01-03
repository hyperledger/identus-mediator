package fmgp.did.comm.extension

import zio.json._

// Lang Extension: https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/l10n/main.md

type LanguageCodeIANA = String

case class L10n(inline: Option[Seq[L10nInline]], service: Option[L10nService], table: Option[L10nTable])
object L10n {
  given decoder: JsonDecoder[L10n] = DeriveJsonDecoder.gen[L10n]
  given encoder: JsonEncoder[L10n] = DeriveJsonEncoder.gen[L10n]
}

case class L10nInline(lang: LanguageCodeIANA, field: JsonPath, translation: String)
object L10nInline {
  given decoder: JsonDecoder[L10nInline] = DeriveJsonDecoder.gen[L10nInline]
  given encoder: JsonEncoder[L10nInline] = DeriveJsonEncoder.gen[L10nInline]
}

type JsonPath = String

/** TODO Not currently defined, but reserved for use with a URI that allows calls to a web service that provides
  * translations.
  */
type L10nService = String

/** TODO Not currently defined, but reserved for use with a URI that allows a localized message lookup table to be
  * downloaded.
  */
type L10nTable = String
