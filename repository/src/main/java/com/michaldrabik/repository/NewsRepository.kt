package com.michaldrabik.repository

import com.michaldrabik.common.di.AppScope
import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.common.extensions.toMillis
import com.michaldrabik.data_local.database.AppDatabase
import com.michaldrabik.data_remote.Cloud
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.NewsItem
import com.michaldrabik.ui_model.NewsItem.Type.MOVIE
import com.michaldrabik.ui_model.NewsItem.Type.SHOW
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AppScope
class NewsRepository @Inject constructor(
  private val cloud: Cloud,
  private val database: AppDatabase,
  private val mappers: Mappers,
) {

  companion object {
    const val VALID_CACHE_MINUTES = 60L
  }

  suspend fun getCachedNews(type: NewsItem.Type) =
    database.newsDao().getAllByType(type.slug)
      .map { mappers.news.fromDatabase(it) }

  suspend fun loadShowsNews(token: RedditAuthToken, forceRefresh: Boolean): List<NewsItem> {
    if (!forceRefresh) {
      val cachedNews = getCachedNews(SHOW)
      val cacheTimestamp = cachedNews.firstOrNull()?.createdAt?.toMillis() ?: 0

      val isCacheValid = nowUtcMillis() - cacheTimestamp <= TimeUnit.MINUTES.toMillis(VALID_CACHE_MINUTES)
      if (isCacheValid && getCachedNews(SHOW).isNotEmpty()) {
        return cachedNews.toList()
      }
    }

    val remoteItems = cloud.redditApi.fetchTelevision(token.token)
      .filterNot { it.is_self }
      .map { mappers.news.fromNetwork(it, SHOW) }

    val dbItems = remoteItems.map { mappers.news.toDatabase(it) }
    database.newsDao().replaceForType(dbItems, SHOW.slug)

    return remoteItems.toList()
  }

  suspend fun loadMoviesNews(token: RedditAuthToken, forceRefresh: Boolean): List<NewsItem> {
    if (!forceRefresh) {
      val cachedNews = getCachedNews(MOVIE)
      val cacheTimestamp = cachedNews.firstOrNull()?.createdAt?.toMillis() ?: 0

      val isCacheValid = nowUtcMillis() - cacheTimestamp <= TimeUnit.MINUTES.toMillis(VALID_CACHE_MINUTES)
      if (isCacheValid && getCachedNews(MOVIE).isNotEmpty()) {
        return cachedNews.toList()
      }
    }

    val remoteItems = cloud.redditApi.fetchMovies(token.token)
      .filterNot { it.is_self }
      .map { mappers.news.fromNetwork(it, MOVIE) }

    val dbItems = remoteItems.map { mappers.news.toDatabase(it) }
    database.newsDao().replaceForType(dbItems, MOVIE.slug)

    return remoteItems.toList()
  }
}