package mx.edu.utez.mediaapp.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ejemplo.mediaapp.data.SettingsRepository

class PlaybackViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaybackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val repository = SettingsRepository(application)
            return PlaybackViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}