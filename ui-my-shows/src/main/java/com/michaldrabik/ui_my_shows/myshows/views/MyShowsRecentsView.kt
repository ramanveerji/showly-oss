package com.michaldrabik.ui_my_shows.myshows.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.GridLayout
import androidx.core.view.updatePadding
import com.michaldrabik.ui_base.common.ListViewMode
import com.michaldrabik.ui_base.common.ListViewMode.GRID
import com.michaldrabik.ui_base.common.ListViewMode.GRID_TITLE
import com.michaldrabik.ui_base.common.ListViewMode.LIST_COMPACT
import com.michaldrabik.ui_base.common.ListViewMode.LIST_NORMAL
import com.michaldrabik.ui_base.utilities.extensions.dimenToPx
import com.michaldrabik.ui_base.utilities.extensions.isTablet
import com.michaldrabik.ui_base.utilities.extensions.screenWidth
import com.michaldrabik.ui_my_shows.R
import com.michaldrabik.ui_my_shows.databinding.ViewMyShowsRecentsBinding
import com.michaldrabik.ui_my_shows.myshows.recycler.MyShowsItem

class MyShowsRecentsView : FrameLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewMyShowsRecentsBinding.inflate(LayoutInflater.from(context), this)

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    clipChildren = false
  }

  private val itemHeight by lazy { context.dimenToPx(R.dimen.myShowsFanartHeight) }
  private val itemMargin by lazy { context.dimenToPx(R.dimen.spaceTiny) }
  private val itemWidth by lazy {
    val space = if (context.isTablet()) {
      context.dimenToPx(R.dimen.myShowsRecyclerHorizontalPadding) * 4
    } else {
      context.dimenToPx(R.dimen.screenMarginHorizontal) * 2
    }
    ((screenWidth() - space) / 2) - itemMargin
  }

  fun bind(
    item: MyShowsItem.RecentsSection,
    viewMode: ListViewMode,
    itemClickListener: ((MyShowsItem) -> Unit)?,
    itemLongClickListener: ((MyShowsItem) -> Unit)?,
  ) {
    binding.myShowsRecentsContainer.removeAllViews()
    setPaddings(viewMode)

    val clickListener: (MyShowsItem) -> Unit = { itemClickListener?.invoke(it) }
    val longClickListener: (MyShowsItem, View) -> Unit = { i, _ -> itemLongClickListener?.invoke(i) }

    item.items.forEachIndexed { index, showItem ->
      val view = MyShowFanartView(context).apply {
        layoutParams = LayoutParams(0, MATCH_PARENT)
        bind(showItem, clickListener, longClickListener)
      }
      val layoutParams = GridLayout.LayoutParams().apply {
        width = itemWidth
        height = itemHeight
        columnSpec = GridLayout.spec(index % 2, 1F)
        if (index % 2 == 0) {
          setMargins(0, itemMargin, itemMargin, itemMargin)
        } else {
          setMargins(itemMargin, itemMargin, 0, itemMargin)
        }
      }
      binding.myShowsRecentsContainer.addView(view, layoutParams)
    }
  }

  private fun setPaddings(viewMode: ListViewMode) {
    with(binding) {
      val padding = when (viewMode) {
        GRID, GRID_TITLE -> {
          resources.getDimensionPixelSize(R.dimen.myShowsRecentsGridPadding)
        }
        LIST_NORMAL, LIST_COMPACT -> {
          if (context.isTablet()) {
            resources.getDimensionPixelSize(R.dimen.myShowsRecyclerHorizontalPadding)
          } else {
            resources.getDimensionPixelSize(R.dimen.screenMarginHorizontal)
          }
        }
      }
      myShowsRecentsContainer.updatePadding(left = padding, right = padding)
    }
  }
}
