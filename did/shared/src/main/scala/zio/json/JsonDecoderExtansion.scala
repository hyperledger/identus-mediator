package zio.json

import zio.json.internal._
import zio.json.ast.Json

extension [A](self: JsonDecoder[A])
  def orElseAndWarpErrors[A1 >: A](that: => JsonDecoder[A1]): JsonDecoder[A1] =
    new JsonDecoder[A1] {

      def unsafeDecode(trace: List[JsonError], in: RetractReader): A1 = {
        val in2 = new zio.json.internal.WithRecordingReader(in, 64)

        try self.unsafeDecode(trace, in2)
        catch {
          case JsonDecoder.UnsafeJson(traceOut1) =>
            in2.rewind()
            try that.unsafeDecode(trace, in2)
            catch {
              case JsonDecoder.UnsafeJson(traceOut2) =>
                throw JsonDecoder.UnsafeJson(
                  List(JsonError.Message(JsonError.render(traceOut1) + " OR " + JsonError.render(traceOut2)))
                )
            }
          case _: UnexpectedEnd =>
            in2.rewind()
            that.unsafeDecode(trace, in2)
        }
      }

      override final def unsafeFromJsonAST(trace: List[JsonError], json: Json): A1 =
        try self.unsafeFromJsonAST(trace, json)
        catch {
          case JsonDecoder.UnsafeJson(_) | _: UnexpectedEnd => that.unsafeFromJsonAST(trace, json)
        }

      override def unsafeDecodeMissing(trace: List[JsonError]): A1 =
        try self.unsafeDecodeMissing(trace)
        catch {
          case _: Throwable => that.unsafeDecodeMissing(trace)
        }

    }
