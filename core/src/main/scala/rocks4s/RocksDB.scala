package rocks4s

import cats._
import cats.effect._
import cats.implicits._

import fs2._

import org.rocksdb.{RocksDB => JavaRocksDB, _}
import org.rocksdb.TransactionLogIterator.BatchResult

import scala.util.control.NonFatal

final class RocksDB[F[_]: Sync] private (client: JavaRocksDB) {
  import codec._

  def backup(flushBeforeBackup: Boolean = true): F[Unit] =
    engineF.use { engine =>
      F.delay(engine.createNewBackup(client, flushBeforeBackup))
    }

  def checkpoint(path: String): Resource[F, Unit] =
    bracketRef(
      F.delay(Checkpoint.create(client))
    ).evalMap(cp => F.delay(cp.createCheckpoint(path)))

  def createColumnFamily(name: String): Resource[F, ColumnFamilyHandle] =
    bracketRef(
      F.delay(client.createColumnFamily(new ColumnFamilyDescriptor(name.getBytes())))
    )

  def compact: F[Unit] =
    F.delay(client.compactRange())

  def defaultColumnFamily: Resource[F, ColumnFamilyHandle] =
    bracketRef(
      F.delay(client.getDefaultColumnFamily())
    )

  def dropColumnFamily(handle: ColumnFamilyHandle): F[Unit] =
    F.delay(client.dropColumnFamily(handle))

  def flush(wait: Boolean = false): F[Unit] =
    bracketRef(
      F.delay(new FlushOptions())
        .flatTap(fo => F.delay(fo.setWaitForFlush(wait)))
    ).evalMap(fo => F.delay(client.flush(fo)))
      .use(_ => F.unit)

  val engineF: Resource[F, BackupEngine] = for {
    env <- bracketRef(F.delay(Env.getDefault()))
    opt <- bracketRef(F.delay(new BackupableDBOptions(client.getProperty("path"))))
    engine <- bracketRef(F.delay(BackupEngine.open(env, opt)))
  } yield engine

  def get[K, V](key: K)(
    implicit keyEncoder: Encoder[F, K],
    valueDecoder: Decoder[F, V]
  ): F[Option[V]] =
    for {
      k <- keyEncoder.encode(key).compile.to[Array]
      v <- F.delay(client.get(k)).map(Option(_))
      v <- v.traverse(bs => valueDecoder.decode(Stream.emits(bs)))
    } yield v

  def latestSequenceNumber: F[Long] =
    F.delay(client.getLatestSequenceNumber())

  def property(name: String): F[String] =
    F.delay(client.getProperty(name))

  def put[K, V](key: K, value: V)(
    implicit keyEncoder: Encoder[F, K],
    valueEncoder: Encoder[F, V]
  ): F[Unit] =
    for {
      k <- keyEncoder.encode(key).compile.to[Array]
      v <- valueEncoder.encode(value).compile.to[Array]
      _ <- F.delay(client.put(k, v))
    } yield ()

  def remove[K](key: K)(
    implicit keyEncoder: Encoder[F, K]
  ): F[Unit] =
    keyEncoder.encode(key).compile.to[Array] >>= (k => F.delay(client.remove(k)))

  def snapshot: Resource[F, Snapshot] =
    Resource.make(
      F.delay(client.getSnapshot())
    )(ss => F.delay(ss.close()) *> F.delay(client.releaseSnapshot(ss)))

  def write(options: WriteOptions, batch: WriteBatch): F[Unit] =
    F.delay(client.write(options, batch))

  def close: F[Unit] = F.delay(client.close())

  private[this] def bracketRef[A <: AbstractNativeReference](fa: F[A]): Resource[F, A] =
    Resource.make(fa)(a => F.delay(a.close()))

  private[this] def F[F[_]](implicit F: Sync[F]): F.type = F
}

object RocksDB {

  def apply[F[_]: Sync](
    path: String,
    options: Options,
    ttl: Boolean = false,
  ): Resource[F, RocksDB[F]] =
    Resource
      .make(applyF(path, options, ttl))(_.close)

  def applyF[F[_]: Sync](
    path: String,
    options: Options,
    ttl: Boolean = false,
  ): F[RocksDB[F]] =
    F.delay(JavaRocksDB.loadLibrary()) *> F
      .delay(
        if (ttl)
          TtlDB.open(options, path)
        else
          JavaRocksDB.open(options, path)
      )
      .map(new RocksDB[F](_))

  private[this] def F[F[_]](implicit F: Sync[F]): F.type = F
}
