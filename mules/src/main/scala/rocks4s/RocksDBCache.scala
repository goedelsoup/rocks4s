package rocks4s

import cats.effect._
import cats.implicits._
import io.chrisdavenport.mules._
import rocks4s.codec._
import org.rocksdb.{Options => JavaOptions, RocksDBException}

final class RocksDBCache[F[_], K, V] private[RocksDBCache] (
  private val rocks: RocksDB[F],
  private val onInsert: (K, V) => F[Unit],
  private val onCacheHit: (K, V) => F[Unit],
  private val onCacheMiss: K => F[Unit],
  private val onDelete: K => F[Unit]
)(implicit val F: Sync[F], val EK: Encoder[F, K], val EV: Encoder[F, V], val DV: Decoder[F, V])
    extends Cache[F, K, V] {
  import RocksDBCache.RocksDBCacheItem

  def delete(k: K): F[Unit] = rocks.remove(k)

  def insert(k: K, v: V): F[Unit] = rocks.put(k, v)

  def lookup(k: K): F[Option[V]] = rocks.get(k)

  def withOnCacheHit(onCacheHitNew: (K, V) => F[Unit]): RocksDBCache[F, K, V] =
    new RocksDBCache[F, K, V](
      rocks,
      onInsert, { (k, v) =>
        onCacheHit(k, v) >> onCacheHitNew(k, v)
      },
      onCacheMiss,
      onDelete
    )

  def withOnCacheMiss(onCacheMissNew: K => F[Unit]): RocksDBCache[F, K, V] =
    new RocksDBCache[F, K, V](
      rocks,
      onInsert,
      onCacheHit, { k =>
        onCacheMiss(k) >> onCacheMissNew(k)
      },
      onDelete
    )

  def withOnDelete(onDeleteNew: K => F[Unit]): RocksDBCache[F, K, V] =
    new RocksDBCache[F, K, V](
      rocks,
      onInsert,
      onCacheHit,
      onCacheMiss, { k =>
        onDelete(k) >> onDeleteNew(k)
      }
    )

  def withOnInsert(onInsertNew: (K, V) => F[Unit]): RocksDBCache[F, K, V] =
    new RocksDBCache[F, K, V](
      rocks, { (k, v) =>
        onInsert(k, v) >> onInsertNew(k, v)
      },
      onCacheHit,
      onCacheMiss,
      onDelete
    )
}

object RocksDBCache {

  private case class RocksDBCacheItem[A](
    item: A,
    itemExpiration: Option[TimeSpec]
  )

  def apply[F[_]: Sync: Clock, K, V](
    path: String,
    options: JavaOptions,
    defaultExpiration: Option[TimeSpec]
  )(implicit EK: Encoder[F, K], EV: Encoder[F, V], DV: Decoder[F, V]): Resource[F, RocksDBCache[F, K, V]] =
    RocksDB(path, options, ttl = true)
      .map(
        new RocksDBCache[F, K, V](
          _, { (_, _) =>
            Sync[F].unit
          }, { (_, _) =>
            Sync[F].unit
          }, { _: K =>
            Sync[F].unit
          }, { _: K =>
            Sync[F].unit
          }
        )
      )
}
