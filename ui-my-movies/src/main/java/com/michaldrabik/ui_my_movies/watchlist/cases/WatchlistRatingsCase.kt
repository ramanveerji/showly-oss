package com.michaldrabik.ui_my_movies.watchlist.cases

import com.michaldrabik.repository.RatingsRepository
import com.michaldrabik.repository.UserTraktManager
import com.michaldrabik.ui_model.IdTrakt
import com.michaldrabik.ui_model.TraktRating
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class WatchlistRatingsCase @Inject constructor(
  private val ratingsRepository: RatingsRepository,
  private val userTraktManager: UserTraktManager,
) {

  suspend fun loadRatings(): Map<IdTrakt, TraktRating?> = withContext(Dispatchers.IO) {
    if (!userTraktManager.isAuthorized()) {
      return@withContext emptyMap()
    }
    ratingsRepository.movies.loadMoviesRatings().associateBy { it.idTrakt }
  }
}
