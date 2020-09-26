package com.michaldrabik.showly2.ui.watchlist.cases

import android.content.Context
import com.michaldrabik.common.di.AppScope
import com.michaldrabik.showly2.common.trakt.quicksync.QuickSyncManager
import com.michaldrabik.showly2.ui.show.seasons.episodes.EpisodesManager
import com.michaldrabik.showly2.ui.watchlist.recycler.WatchlistItem
import com.michaldrabik.ui_model.EpisodeBundle
import javax.inject.Inject

@AppScope
class WatchlistEpisodesCase @Inject constructor(
  private val episodesManager: EpisodesManager,
  private val quickSyncManager: QuickSyncManager
) {

  suspend fun setEpisodeWatched(context: Context, item: WatchlistItem) {
    val bundle = EpisodeBundle(item.episode, item.season, item.show)
    episodesManager.setEpisodeWatched(bundle)
    quickSyncManager.scheduleEpisodes(context, listOf(item.episode.ids.trakt.id))
  }
}
