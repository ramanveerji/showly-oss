package com.michaldrabik.storage.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.michaldrabik.storage.database.model.Season

@Dao
interface SeasonsDao : BaseDao<Season> {

  @Query("SELECT * FROM seasons WHERE id_show_trakt IN (:traktIds)")
  suspend fun getAllForShows(traktIds: List<Long>): List<Season>

  @Query("SELECT * FROM seasons WHERE id_show_trakt IN (:traktIds) AND is_watched = 1")
  suspend fun getAllWatchedForShows(traktIds: List<Long>): List<Season>

  @Query("SELECT id_trakt FROM seasons WHERE id_show_trakt IN (:traktIds) AND is_watched = 1")
  suspend fun getAllWatchedIdsForShows(traktIds: List<Long>): List<Long>

  @Query("SELECT * FROM seasons WHERE id_show_trakt = :traktId")
  suspend fun getAllByShowId(traktId: Long): List<Season>

  @Query("SELECT * FROM seasons WHERE id_trakt = :traktId")
  suspend fun getById(traktId: Long): Season?

  @Query("SELECT * FROM seasons WHERE id_show_trakt = :showTraktId")
  suspend fun getAllForShow(showTraktId: Long): List<Season>

  @Transaction
  suspend fun upsert(items: List<Season>) {
    val result = insert(items)
    val updateList = mutableListOf<Season>()

    result.forEachIndexed { index, id ->
      if (id == -1L) updateList.add(items[index])
    }

    if (updateList.isNotEmpty()) update(updateList)
  }

  @Query("DELETE FROM seasons WHERE id_show_trakt = :showTraktId AND id_trakt IN(:seasonsIds)")
  suspend fun deleteForShow(seasonsIds: List<Long>, showTraktId: Long)
}
