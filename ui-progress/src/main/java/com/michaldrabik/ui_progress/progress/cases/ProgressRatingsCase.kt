package com.michaldrabik.ui_progress.progress.cases

import com.michaldrabik.repository.UserTraktManager
import com.michaldrabik.repository.settings.SettingsRepository
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class ProgressRatingsCase @Inject constructor(
  private val userTraktManager: UserTraktManager,
  private val settingsRepository: SettingsRepository,
) {

  suspend fun isQuickRateEnabled(): Boolean = withContext(Dispatchers.IO) {
    val isSignedIn = userTraktManager.isAuthorized()
    val isPremium = settingsRepository.isPremium
    val isQuickRate = settingsRepository.load().traktQuickRateEnabled
    isPremium && isSignedIn && isQuickRate
  }
}
