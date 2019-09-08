# rocks4s
Pure functional wrapper for RocksDB

---

#### Features

##### Frontends

- [x] [Mules]()

##### Serialization
- [ ] Avro
- [x] [Circe]()
- [ ] Protocol Buffers

##### Persistence
- [ ] AWS S3
- [ ] Azure Blob Storage
- [ ] GCP Cloud Storage

##### Distributed Data
- [ ] etcd
- [ ] Consul

---

### Usage

Construct a database instance and running operations:

```scala
import cats._
import cats.effect._
import cats.implicits._

import rocks4s._
import org.rocksdb.Options

object Example extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    RocksDB[IO](".rocks", new Options().setCreateOnMissing(true))
      .use { rocks =>
        for {
          _ <- rocks.put("foo", "bar")
          v <- rocks.get("foo")
          _ <- IO(println(s"Retrieved value $v"))
        } yield ExitCode.Success
      }
}
```

See [the examples project](./examples/src/main/scala/rocks4s/examples) for more thorough usage.