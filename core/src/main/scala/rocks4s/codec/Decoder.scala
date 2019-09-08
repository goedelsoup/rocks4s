package rocks4s.codec

import cats.Functor
import cats.effect.Sync
import cats.implicits._
import fs2.Stream

trait Decoder[F[_], A] { self =>

  def decode(bytes: Stream[F, Byte]): F[A]

  def map[B](f: A => B)(implicit F: Functor[F]): Decoder[F, B] =
    new Decoder[F, B] {
      def decode(bytes: Stream[F, Byte]): F[B] =
        self.decode(bytes).map(f)
    }
}

object Decoder {

  def apply[F[_], A](implicit ev: Decoder[F, A]) = ev

  implicit def decoderForString[F[_]: Sync]: Decoder[F, String] =
    new Decoder[F, String] {
      def decode(bytes: Stream[F, Byte]): F[String] =
        bytes.through(fs2.text.utf8Decode).compile.lastOrError
    }
}
