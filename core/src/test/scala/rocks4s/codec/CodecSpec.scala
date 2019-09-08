package rocks4s.codec

import java.nio.file.Files
import java.util.UUID

import cats.effect._
import cats.effect.concurrent._
import cats.effect.implicits._
import cats.effect.IO
import cats.effect.laws.util.TestContext
import cats.implicits._
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalacheck.Parameters

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class CodecSpec extends Specification with ScalaCheck {

  "Codec" should {

    "encode and decode strings" >> prop { (s: String) =>
      Decoder[IO, String]
        .decode(Encoder[IO, String].encode(s))
        .unsafeRunSync() must_=== s
    }
  }
}
