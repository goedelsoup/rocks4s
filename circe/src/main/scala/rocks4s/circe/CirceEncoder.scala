package rocks4s.circe

import cats.effect.Sync
import fs2.Stream
import fs2.text.utf8Encode
import io.circe.{Encoder => CEncoder}
import rocks4s.codec.Encoder

trait CirceEncoder {
  implicit def circeEncoder[F[_]: Sync, A: CEncoder]: Encoder[F, A] =
    new Encoder[F, A] {
      def encode(a: A): fs2.Stream[F, Byte] =
        Stream
          .emit(CEncoder[A].apply(a).noSpaces)
          .through(utf8Encode)
    }
}

object CirceEncoder extends CirceEncoder
