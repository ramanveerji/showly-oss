package com.michaldrabik.showly2.ui.discover.recycler

import com.michaldrabik.ui_model.Image
import com.michaldrabik.ui_model.Show

data class DiscoverListItem(
  override val show: Show,
  override val image: Image,
  override var isLoading: Boolean = false,
  val isFollowed: Boolean = false,
  val isSeeLater: Boolean = false
) : ListItem
