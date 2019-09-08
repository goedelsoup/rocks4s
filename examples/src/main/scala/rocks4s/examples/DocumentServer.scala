package rocks4s.examples

import cats._
import cats.effect._
import cats.implicits._
import io.chrisdavenport.mules.Cache
import io.circe.Json
import io.circe.syntax._
import rocks4s.RocksDBCache
import rocks4s.circe.CirceCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder
import org.rocksdb.{Options => JavaOptions}

object DocumentServer extends IOApp {

  type Store = RocksDBCache[IO, String, Json]

  def run(args: List[String]): IO[ExitCode] =
    Routes.make
      .use(_.serve.compile.lastOrError)

  final case class Config(
    port: Int,
    database: String,
  )

  object Config {

    import ciris._
    import ciris.api._
    import ciris.cats.effect._

    def loadF: Resource[IO, Config] =
      Resource.liftF(
        loadConfig(
          envF[IO, Int]("SERVER_PORT").orValue(8080),
          envF[IO, String]("DATABASE").orValue(".rocks")
        )(Config.apply).orRaiseThrowable
      )
  }

  object Routes extends Http4sDsl[IO] {

    def make(
      implicit CE: ConcurrentEffect[IO]
    ): Resource[IO, BlazeServerBuilder[IO]] = {

      import org.http4s.HttpRoutes
      import org.http4s.circe.CirceEntityCodec._
      import org.http4s.implicits._
      import org.http4s.server.Router

      for {
        config <- Config.loadF
        cache <- RocksDBCache[IO, String, Json](config.database,
                                                new JavaOptions()
                                                  .setCreateIfMissing(true),
                                                None)
        server = BlazeServerBuilder[IO]
          .bindHttp(port = config.port)
          .withHttpApp(
            Router(
              "/" -> HttpRoutes.of[IO] {
                case HEAD -> Root / key =>
                  cache
                    .lookup(key)
                    .void
                    .as(NoContent())
                    .flatten

                case GET -> Root / key =>
                  cache.lookup(key) >>= {
                    case None         => NotFound()
                    case Some(result) => Ok(result)
                  }

                case req @ PUT -> Root / key =>
                  req
                    .attemptAs[Json]
                    .value >>= {
                    case Left(error)  => BadRequest(error.message)
                    case Right(value) => cache.insert(key, value) >>= (Ok(_))
                  }

                case DELETE -> Root / key => cache.delete(key) >>= (Ok(_))
              },
            ).orNotFound
          )
      } yield server
    }
  }
}
