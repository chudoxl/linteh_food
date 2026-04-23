package com.github.chudoxl.linteh.food.screens.about

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.parseMarkdownFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class AboutViewModel(application: Application) : AndroidViewModel(application) {

    val markdownState: StateFlow<State> = flow {
        val readme = runCatching {
            getApplication<Application>().assets.open("readme.md")
                .bufferedReader().use { it.readText() }
        }.getOrElse { "_Не удалось загрузить описание приложения._" }
        emitAll(parseMarkdownFlow(readme))
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading())
}
