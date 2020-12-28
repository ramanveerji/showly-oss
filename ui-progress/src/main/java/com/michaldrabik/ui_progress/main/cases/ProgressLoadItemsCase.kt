package com.michaldrabik.ui_progress.main.cases

import com.michaldrabik.common.Config
import com.michaldrabik.common.di.AppScope
import com.michaldrabik.common.extensions.nowUtc
import com.michaldrabik.storage.database.AppDatabase
import com.michaldrabik.storage.database.model.EpisodeWatchlist
import com.michaldrabik.ui_model.Episode
import com.michaldrabik.ui_model.Image
import com.michaldrabik.ui_model.ImageType
import com.michaldrabik.ui_model.Show
import com.michaldrabik.ui_model.SortOrder
import com.michaldrabik.ui_model.SortOrder.EPISODES_LEFT
import com.michaldrabik.ui_model.SortOrder.NAME
import com.michaldrabik.ui_model.SortOrder.RECENTLY_WATCHED
import com.michaldrabik.ui_model.Translation
import com.michaldrabik.ui_progress.ProgressItem
import com.michaldrabik.ui_repository.PinnedItemsRepository
import com.michaldrabik.ui_repository.TranslationsRepository
import com.michaldrabik.ui_repository.mappers.Mappers
import com.michaldrabik.ui_repository.shows.ShowsRepository
import org.threeten.bp.temporal.ChronoUnit.DAYS
import java.util.Locale.ROOT
import javax.inject.Inject

@AppScope
class ProgressLoadItemsCase @Inject constructor(
  private val database: AppDatabase,
  private val mappers: Mappers,
  private val showsRepository: ShowsRepository,
  private val translationsRepository: TranslationsRepository,
  private val pinnedItemsRepository: PinnedItemsRepository
) {

  suspend fun loadMyShows() = showsRepository.myShows.loadAll()

  suspend fun loadProgressItem(show: Show): ProgressItem {
    val episodes = database.episodesDao().getAllForShowWatchlist(show.traktId)
      .filter { it.seasonNumber != 0 }
    val seasons = database.seasonsDao().getAllForShow(show.traktId)
      .filter { it.seasonNumber != 0 }

    val episodesCount = episodes.count()
    val unwatchedEpisodes = episodes.filter { !it.isWatched }
    val unwatchedEpisodesCount = unwatchedEpisodes.count()

    val nextEpisode = unwatchedEpisodes
      .filter { it.firstAired != null }
      .sortedWith(compareBy<EpisodeWatchlist> { it.seasonNumber }.thenBy { it.episodeNumber })
      .firstOrNull() ?: return ProgressItem.EMPTY

    val upcomingEpisode = unwatchedEpisodes
      .filter { it.firstAired != null }
      .sortedBy { it.firstAired }
      .firstOrNull {
        it.firstAired!!.isAfter(nowUtc()) ||
          it.firstAired!!.truncatedTo(DAYS) == nowUtc().truncatedTo(DAYS)
      }

    val isPinned = pinnedItemsRepository.isItemPinned(show)
    val season = seasons.first { it.idTrakt == nextEpisode.idSeason }
    val episode = database.episodesDao().getById(nextEpisode.idTrakt)
    val episodeUi = mappers.episode.fromDatabase(episode)
    val upEpisode = upcomingEpisode?.let {
      val epDb = database.episodesDao().getById(it.idTrakt)
      mappers.episode.fromDatabase(epDb)
    } ?: Episode.EMPTY

    var showTranslation: Translation? = null
    var episodeTranslation: Translation? = null
    var upcomingTranslation: Translation? = null

    val language = translationsRepository.getLanguage()
    if (language != Config.DEFAULT_LANGUAGE) {
      showTranslation = translationsRepository.loadTranslation(show, language, true)
      episodeTranslation = translationsRepository.loadTranslation(episodeUi, show.ids.trakt, language, true)
      upcomingTranslation = translationsRepository.loadTranslation(upEpisode, show.ids.trakt, language, true)
    }

    return ProgressItem(
      show,
      mappers.season.fromDatabase(season),
      episodeUi,
      upEpisode,
      Image.createUnavailable(ImageType.POSTER),
      episodesCount,
      episodesCount - unwatchedEpisodesCount,
      isPinned = isPinned,
      showTranslation = showTranslation,
      episodeTranslation = episodeTranslation,
      upcomingEpisodeTranslation = upcomingTranslation
    )
  }

  fun prepareWatchlistItems(
    input: List<ProgressItem>,
    searchQuery: String,
    sortOrder: SortOrder
  ): List<ProgressItem> {
    val items = input
      .filter { it.episodesCount != 0 && it.episode.firstAired != null }
      .groupBy { it.episode.hasAired(it.season) }

    val aired = (items[true] ?: emptyList())
      .sortedWith(
        when (sortOrder) {
          NAME -> compareBy {
            val translatedTitle = if (it.showTranslation?.hasTitle == false) null else it.showTranslation?.title
            (translatedTitle ?: it.show.title).toUpperCase(ROOT)
          }
          RECENTLY_WATCHED -> compareByDescending { it.show.updatedAt }
          EPISODES_LEFT -> compareBy { it.episodesCount - it.watchedEpisodesCount }
          else -> throw IllegalStateException("Invalid sort order")
        }
      )

    val notAired = (items[false] ?: emptyList())
      .sortedBy { it.episode.firstAired?.toInstant()?.toEpochMilli() }

    return (aired + notAired)
      .filter {
        if (searchQuery.isBlank()) true
        else it.show.title.contains(searchQuery, true) ||
          it.episode.title.contains(searchQuery, true) ||
          it.showTranslation?.title?.contains(searchQuery, true) == true ||
          it.episodeTranslation?.title?.contains(searchQuery, true) == true
      }
  }
}
