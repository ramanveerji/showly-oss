package com.michaldrabik.showly2.ui.show.quickSetup.views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import com.michaldrabik.showly2.R
import com.michaldrabik.showly2.utilities.extensions.colorFromAttr
import com.michaldrabik.showly2.utilities.extensions.onClick
import com.michaldrabik.ui_model.Episode
import kotlinx.android.synthetic.main.view_quick_setup_item.view.*

class QuickSetupItemView : FrameLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  init {
    inflate(context, R.layout.view_quick_setup_item, this)
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
  }

  fun bind(
    item: Episode,
    isChecked: Boolean,
    onItemClickListener: ((Episode, Boolean) -> Unit)?
  ) {
    val titleColor = if (isChecked) R.attr.colorAccent else android.R.attr.textColorSecondary
    val subTitleColor = if (isChecked) R.attr.colorAccent else android.R.attr.textColorPrimary

    viewQuickSetupItemRadio.isChecked = isChecked
    viewQuickSetupItemTitle.run {
      text = context.getString(R.string.textEpisode, item.number)
      setTextColor(context.colorFromAttr(titleColor))
    }
    viewQuickSetupItemSubtitle.run {
      text = item.title
      setTextColor(context.colorFromAttr(subTitleColor))
    }

    onClick { onItemClickListener?.invoke(item, isChecked) }
  }
}
