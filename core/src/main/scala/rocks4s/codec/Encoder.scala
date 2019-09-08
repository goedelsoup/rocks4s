package rocks4s.codec

import cats.effect.Sync
import fs2.Stream

trait Encoder[F[_], A] { self =>

  def encode(a: A): Stream[F, Byte]

  def contramap[B](f: B => A): Encoder[F, B] =
    new Encoder[F, B] {
      def encode(b: B): Stream[F, Byte] =
        self.encode(f(b))
    }
}

object Encoder {

  def apply[F[_], A](implicit ev: Encoder[F, A]) = ev

  implicit def encoderForString[F[_]]: Encoder[F, String] =
    new Encoder[F, String] {
      def encode(a: String): Stream[F, Byte] =
        Stream.emit(a).through(fs2.text.utf8Encode)
    }
}
