package com.michaldrabik.ui_discover

import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.michaldrabik.common.Config.MAIN_GRID_SPAN
import com.michaldrabik.ui_base.BaseFragment
import com.michaldrabik.ui_base.common.OnTabReselectedListener
import com.michaldrabik.ui_base.utilities.extensions.*
import com.michaldrabik.ui_discover.di.UiDiscoverComponentProvider
import com.michaldrabik.ui_discover.recycler.DiscoverAdapter
import com.michaldrabik.ui_discover.recycler.DiscoverListItem
import com.michaldrabik.ui_navigation.java.NavigationArgs.ARG_SHOW_ID
import kotlinx.android.synthetic.main.fragment_discover.*
import kotlin.math.hypot
import kotlin.random.Random

class DiscoverFragment : BaseFragment<DiscoverViewModel>(R.layout.fragment_discover), OnTabReselectedListener {

  override val viewModel by viewModels<DiscoverViewModel> { viewModelFactory }

  private val swipeRefreshStartOffset by lazy { requireContext().dimenToPx(R.dimen.swipeRefreshStartOffset) }
  private val swipeRefreshEndOffset by lazy { requireContext().dimenToPx(R.dimen.swipeRefreshEndOffset) }

  private lateinit var adapter: DiscoverAdapter
  private lateinit var layoutManager: GridLayoutManager

  override fun onCreate(savedInstanceState: Bundle?) {
    (requireActivity() as UiDiscoverComponentProvider).provideDiscoverComponent().inject(this)
    super.onCreate(savedInstanceState)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupRecycler()
    setupSwipeRefresh()
    setupStatusBar()

    viewModel.run {
      uiLiveData.observe(viewLifecycleOwner, { render(it!!) })
      messageLiveData.observe(viewLifecycleOwner, { showSnack(it) })
      loadDiscoverShows()
    }
  }

  private fun setupView() {
    discoverSearchView.run {
      sortIconVisible = true
      settingsIconVisible = false
      isClickable = false
      onClick { navigateToSearch() }
      onSortClickListener = { toggleFiltersView() }
//      translationY = mainActivity().discoverSearchViewPosition TODO
    }
    discoverMask.onClick { toggleFiltersView() }
    discoverFiltersView.onApplyClickListener = {
      toggleFiltersView()
      viewModel.loadDiscoverShows(
        scrollToTop = true,
        skipCache = true,
        instantProgress = true,
        newFilters = it
      )
    }
    discoverTipFilters.run {
//      fadeIf(!mainActivity().isTipShown(DISCOVER_FILTERS)) TODO
      onClick {
        it.gone()
//        mainActivity().showTip(DISCOVER_FILTERS) TODO
      }
    }
  }

  private fun setupRecycler() {
    layoutManager = GridLayoutManager(context, MAIN_GRID_SPAN)
    adapter = DiscoverAdapter().apply {
      missingImageListener = { ids, force -> viewModel.loadMissingImage(ids, force) }
      itemClickListener = { navigateToDetails(it) }
      listChangeListener = { discoverRecycler.scrollToPosition(0) }
    }
    discoverRecycler.apply {
      adapter = this@DiscoverFragment.adapter
      layoutManager = this@DiscoverFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      setHasFixedSize(true)
    }
  }

  private fun setupSwipeRefresh() {
    discoverSwipeRefresh.apply {
      val color = requireContext().colorFromAttr(R.attr.colorNotification)
      setProgressBackgroundColorSchemeColor(requireContext().colorFromAttr(R.attr.colorSearchViewBackground))
      setColorSchemeColors(color, color, color)
      setOnRefreshListener {
//        mainActivity().discoverSearchViewPosition = 0F TODO
        viewModel.loadDiscoverShows(pullToRefresh = true)
      }
    }
  }

  private fun setupStatusBar() {
    discoverRoot.doOnApplyWindowInsets { _, insets, _, _ ->
      val statusBarSize = insets.systemWindowInsetTop
      discoverRecycler
        .updatePadding(top = statusBarSize + dimenToPx(R.dimen.discoverRecyclerPadding))
      (discoverSearchView.layoutParams as MarginLayoutParams)
        .updateMargins(top = statusBarSize + dimenToPx(R.dimen.spaceSmall))
      (discoverFiltersView.layoutParams as MarginLayoutParams)
        .updateMargins(top = statusBarSize + dimenToPx(R.dimen.searchViewHeight))
      discoverTipFilters.translationY = statusBarSize.toFloat()
      discoverSwipeRefresh.setProgressViewOffset(
        true,
        swipeRefreshStartOffset + statusBarSize,
        swipeRefreshEndOffset
      )
    }
  }

  private fun navigateToSearch() {
    disableUi()
    saveUi()
    hideNavigation()
    discoverFiltersView.fadeOut()
    discoverRecycler.fadeOut(duration = 200) {
      enableUi()
      super.navigateTo(R.id.actionDiscoverFragmentToSearchFragment, null)
    }
  }

  private fun navigateToDetails(item: DiscoverListItem) {
    disableUi()
    saveUi()
    hideNavigation()
    animateItemsExit(item)
    discoverSearchView.fadeOut()
    discoverFiltersView.fadeOut()
  }

  private fun animateItemsExit(item: DiscoverListItem) {
    val clickedIndex = adapter.indexOf(item)
    (0..adapter.itemCount).forEach {
      if (it != clickedIndex) {
        val view = discoverRecycler.findViewHolderForAdapterPosition(it)
        view?.let { v ->
          val randomDelay = Random.nextLong(50, 200)
          v.itemView.fadeOut(duration = 150, startDelay = randomDelay)
        }
      }
    }
    val clickedView = discoverRecycler.findViewHolderForAdapterPosition(clickedIndex)
    clickedView?.itemView?.fadeOut(duration = 150, startDelay = 350, endAction = {
      enableUi()
      val bundle = Bundle().apply { putLong(ARG_SHOW_ID, item.show.ids.trakt.id) }
      navigateTo(R.id.actionDiscoverFragmentToShowDetailsFragment, bundle)
    })
  }

  private fun toggleFiltersView() {
    val delta = dimenToPx(R.dimen.searchViewHeight)
    val cx = discoverFiltersView.width
    val cy = discoverFiltersView.height + dimenToPx(R.dimen.searchViewHeight)
    val radius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
    if (!discoverFiltersView.isVisible) {
      val anim = ViewAnimationUtils.createCircularReveal(discoverFiltersView, cx, -delta, 0F, radius)
      discoverFiltersView.visible()
      discoverMask.fadeIn()
      anim.start()
    } else {
      ViewAnimationUtils.createCircularReveal(discoverFiltersView, cx, -delta, radius, 0F).apply {
        doOnEnd { discoverFiltersView.invisible() }
        start()
      }
      discoverMask.fadeOut()
    }
  }

  private fun saveUi() {
//    mainActivity().discoverSearchViewPosition = discoverSearchView.translationY TODO
  }

  private fun render(uiModel: DiscoverUiModel) {
    uiModel.run {
      shows?.let {
        adapter.setItems(it, scrollToTop == true)
        layoutManager.withSpanSizeLookup { pos -> adapter.getItems()[pos].image.type.spanSize }
        discoverRecycler.fadeIn()
      }
      showLoading?.let {
        discoverSearchView.isClickable = !it
        discoverSearchView.sortIconClickable = !it
        discoverSearchView.isEnabled = !it
        discoverSwipeRefresh.isRefreshing = it
      }
      filters?.let {
        discoverFiltersView.run {
          if (!this.isVisible) bind(it)
        }
        discoverSearchView.iconBadgeVisible = !it.isDefault()
      }
    }
  }

  override fun onTabReselected() = navigateToSearch()
}
