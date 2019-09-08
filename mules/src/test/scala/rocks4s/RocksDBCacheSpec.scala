package rocks4s

import java.nio.file.Files
import java.util.UUID

import cats.effect._
import cats.effect.concurrent._
import cats.effect.implicits._
import cats.effect.IO
import cats.effect.laws.util.TestContext
import cats.implicits._
import org.rocksdb.{Options => JavaOptions, RocksDBException}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalacheck.Parameters

import scala.concurrent.duration._

class RocksDBCacheSpec extends Specification with ScalaCheck {

  "RocksDBCache" should {
    val ctx = TestContext()
    implicit val C = ctx.timer[IO]

    "insert" in {
      Resource
        .liftF(IO(Files.createTempDirectory(s".rocks-${UUID.randomUUID()}")))
        .flatMap(
          d =>
            RocksDBCache[IO, String, String](d.toAbsolutePath().toString(),
                                             new JavaOptions().setCreateIfMissing(true),
                                             None)
        )
        .use { cache =>
          cache.insert("foo", "bar")
        }
        .unsafeRunSync() must_=== ()
    }

    "lookup" in {
      Resource
        .liftF(IO(Files.createTempDirectory(s".rocks-${UUID.randomUUID()}")))
        .flatMap(
          d =>
            RocksDBCache[IO, String, String](d.toAbsolutePath().toString(),
                                             new JavaOptions().setCreateIfMissing(true),
                                             None)
        )
        .use { cache =>
          cache.insert("foo", "bar") >> cache.lookup("foo")
        }
        .unsafeRunSync() must_=== Some("bar")
    }

    "delete" in {
      Resource
        .liftF(IO(Files.createTempDirectory(s".rocks-${UUID.randomUUID()}")))
        .flatMap(
          d =>
            RocksDBCache[IO, String, String](d.toAbsolutePath().toString(),
                                             new JavaOptions().setCreateIfMissing(true),
                                             None)
        )
        .use { cache =>
          cache.delete("foo")
        }
        .unsafeRunSync() must_=== ()
    }
  }
}
