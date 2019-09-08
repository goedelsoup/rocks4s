package rocks4s

import java.nio.file.Files
import java.util.UUID

import cats.effect._
import cats.effect.concurrent._
import cats.effect.implicits._
import cats.effect.IO
import cats.effect.laws.util.TestContext
import cats.implicits._
import io.chrisdavenport.fuuid.FUUID
// import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalacheck.Parameters

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class RocksDBSpec extends Specification with ScalaCheck {

  // private[this] val logger = Slf4jLogger.unsafeCreate[IO]
  implicit val params = Parameters(minTestsOk = 500)

  implicit val arbForFUUID: Arbitrary[FUUID] =
    Arbitrary[FUUID](genForFUUID)
  implicit val genForFUUID: Gen[FUUID] =
    Gen.uuid.map(FUUID.fromUUID)
  implicit val decoderForFUUID: codec.Decoder[IO, FUUID] =
    codec.Decoder.decoderForString[IO].map(FUUID.fromStringOpt(_).get)
  implicit val encoderForFUUID: codec.Encoder[IO, FUUID] =
    codec.Encoder.encoderForString.contramap(_.show)

  "RocksDB" should {

    "never return from an empty cache" >> prop { (k: FUUID) =>
      val ctx = TestContext()
      implicit val CS = ctx.contextShift[IO]

      (for {
        rocks <- RocksDBSpec.makeDatabase
        k <- FUUID.randomFUUID[IO]
        v <- rocks.get(k)
        _ <- rocks.close
      } yield v).unsafeRunSync must_=== None
    }

    "return immediately after insert" >> prop { (k: FUUID, v: FUUID) =>
      val ctx = TestContext()
      implicit val CS = ctx.contextShift[IO]

      (for {
        rocks <- RocksDBSpec.makeDatabase
        _ <- rocks.put(k, v)
        v1 <- rocks.get(k)
        eq = v1.fold(false)(_ === v)
        _ <- rocks.close
      } yield eq).unsafeRunSync must_=== true
    }

    "return immediately after update" >> prop { (k: FUUID, v0: FUUID, v1: FUUID) =>
      val ctx = TestContext()
      implicit val CS = ctx.contextShift[IO]

      (for {
        rocks <- RocksDBSpec.makeDatabase
        _ <- rocks.put(k, v0)
        _ <- rocks.get(k)
        v2 <- rocks.get(k)
        _ <- rocks.put(k, v1)
        v2 <- rocks.get(k)
        eq = v2.fold(false)(_ === v1)
        _ <- rocks.close
      } yield eq).unsafeRunSync must_=== true
    }

    "never return immediately after delete" >> prop { (k: FUUID, v: FUUID) =>
      val ctx = TestContext()
      implicit val CS = ctx.contextShift[IO]

      (for {
        rocks <- RocksDBSpec.makeDatabase
        _ <- rocks.put(k, v)
        v1 <- rocks.get(k)
        _ <- rocks.remove(k)
        v2 <- rocks.get(k)
        _ <- rocks.close
      } yield v2).unsafeRunSync must_=== None
    }
  }
}

object RocksDBSpec {

  def makeDatabase: IO[RocksDB[IO]] =
    IO(
      Files
        .createTempDirectory(s".rocks-${UUID.randomUUID()}")
    ) >>= (
      d =>
        RocksDB.applyF[IO](d.toString,
                           new org.rocksdb.Options()
                             .setCreateIfMissing(true))
    )
}
