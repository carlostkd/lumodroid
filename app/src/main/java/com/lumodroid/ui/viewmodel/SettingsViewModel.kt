package com.lumodroid.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val apiKey: String = "",
    val saved: Boolean = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("lumodroid", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = SettingsState(
            apiKey = prefs.getString("api_key", "") ?: "",
        )
    }

    fun updateApiKey(key: String) { _state.value = _state.value.copy(apiKey = key) }

    fun save() = viewModelScope.launch {
        prefs.edit()
            .putString("api_key", _state.value.apiKey)
            .apply()
        _state.value = _state.value.copy(saved = true)
        kotlinx.coroutines.delay(2000)
        _state.value = _state.value.copy(saved = false)
    }
}
