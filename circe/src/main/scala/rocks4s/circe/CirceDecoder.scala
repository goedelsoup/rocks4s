package rocks4s.circe

import cats.effect.Sync
import cats.implicits._
import fs2.text.utf8Decode
import io.circe.{Decoder => CDecoder}
import io.circe.parser
import rocks4s.codec._

trait CirceDecoder {
  implicit def circeDecoder[F[_]: Sync, A: CDecoder]: Decoder[F, A] =
    new Decoder[F, A] {
      def decode(bytes: fs2.Stream[F, Byte]): F[A] =
        bytes
          .through(utf8Decode)
          .evalMap(parser.decode(_).liftTo[F])
          .compile
          .lastOrError
    }
}

object CirceDecoder extends CirceDecoder
