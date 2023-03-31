package com.michaldrabik.ui_my_shows.hidden.cases

import com.michaldrabik.common.Config
import com.michaldrabik.repository.TranslationsRepository
import com.michaldrabik.ui_model.Show
import com.michaldrabik.ui_model.Translation
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class HiddenTranslationsCase @Inject constructor(
  private val translationsRepository: TranslationsRepository,
) {

  fun getLanguage() = translationsRepository.getLanguage()

  suspend fun loadTranslation(show: Show, onlyLocal: Boolean): Translation? =
    withContext(Dispatchers.IO) {
      val language = getLanguage()
      if (language == Config.DEFAULT_LANGUAGE) {
        return@withContext Translation.EMPTY
      }
      translationsRepository.loadTranslation(show, language, onlyLocal)
    }
}
