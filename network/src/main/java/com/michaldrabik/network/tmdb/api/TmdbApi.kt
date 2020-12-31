package com.michaldrabik.network.tmdb.api

import com.michaldrabik.network.tmdb.model.TmdbActor
import com.michaldrabik.network.tmdb.model.TmdbImages

class TmdbApi(private val service: TmdbService) {

  suspend fun fetchShowImages(tmdbId: Long) =
    try {
      if (tmdbId <= 0) TmdbImages.EMPTY
      service.fetchShowImages(tmdbId)
    } catch (error: Throwable) {
      TmdbImages.EMPTY
    }

  suspend fun fetchEpisodeImage(showTmdbId: Long?, season: Int?, episode: Int?) =
    try {
      if (showTmdbId == null || showTmdbId <= 0) TmdbImages.EMPTY
      if (season == null || season <= 0) TmdbImages.EMPTY
      if (episode == null || episode <= 0) TmdbImages.EMPTY
      val images = service.fetchEpisodeImages(showTmdbId, season, episode)
      images.stills?.firstOrNull()
    } catch (error: Throwable) {
      null
    }

  suspend fun fetchMovieImages(tmdbId: Long) =
    try {
      if (tmdbId <= 0) TmdbImages.EMPTY
      service.fetchMovieImages(tmdbId)
    } catch (error: Throwable) {
      TmdbImages.EMPTY
    }

  suspend fun fetchMovieActors(tmdbId: Long): List<TmdbActor> {
    val result = service.fetchMovieActors(tmdbId)
    return result.cast?.map { it.copy(movieTmdbId = result.id) } ?: emptyList()
  }

  suspend fun fetchShowActors(tmdbId: Long): List<TmdbActor> {
    val result = service.fetchShowActors(tmdbId)
    return result.cast?.map { it.copy(showTmdbId = result.id) } ?: emptyList()
  }
}
