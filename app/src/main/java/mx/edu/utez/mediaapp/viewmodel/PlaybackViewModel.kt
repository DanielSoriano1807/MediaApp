package mx.edu.utez.mediaapp.viewmodel

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import mx.edu.utez.mediaapp.ui.data.SettingsRepository
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaybackViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application), SensorEventListener {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val sensorManager: SensorManager =
        ContextCompat.getSystemService(application, SensorManager::class.java) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val _isAccelerometerEnabled = MutableStateFlow(false)
    val isAccelerometerEnabled: StateFlow<Boolean> = _isAccelerometerEnabled.asStateFlow()

    val currentVolume: StateFlow<Float> = settingsRepository.userVolume
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsRepository.DEFAULT_VOLUME
        )

    init {
        viewModelScope.launch {
            currentVolume.collect { volume ->
                exoPlayer.volume = volume
            }
        }
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }

    fun playMedia(uri: String) {
        val mediaItem = androidx.media3.common.MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun releasePlayer() {
        exoPlayer.release()
        unregisterSensorListener()
    }

    fun toggleAccelerometer() {
        _isAccelerometerEnabled.update { isEnabled ->
            if (!isEnabled) registerSensorListener() else unregisterSensorListener()
            !isEnabled
        }
    }

    private fun registerSensorListener() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun unregisterSensorListener() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val yValue = event.values[1]
            val newVolume = (yValue + 5f) / 10f
            val clampedVolume = newVolume.coerceIn(0.0f, 1.0f)
            setVolume(clampedVolume)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0.0f, 1.0f)
        exoPlayer.volume = clampedVolume
        viewModelScope.launch {
            settingsRepository.saveVolume(clampedVolume)
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}