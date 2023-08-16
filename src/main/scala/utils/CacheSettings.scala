package utils

import akka.actor.ActorSystem
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{Cache, CachingSettings}
import akka.http.scaladsl.model.Uri
import org.mongodb.scala.bson.Document

import scala.concurrent.duration.DurationInt


object CacheSettings {
  def lfuDocumentCache(system: ActorSystem): Cache[Uri, Seq[Document]] = {

    val defaultCachingSettings: CachingSettings = CachingSettings(system)

    val lfuCacheSettings =
      defaultCachingSettings.lfuCacheSettings
        .withInitialCapacity(25)
        .withMaxCapacity(50)
        .withTimeToLive(20.seconds)
        .withTimeToIdle(10.seconds)

      val cachingSettings =
        defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

    val lfuCache: Cache[Uri, Seq[Document]] = LfuCache(cachingSettings)
    lfuCache
  }
}
