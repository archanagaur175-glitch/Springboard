package com.springboard.launcher.data.system

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.MediaMetadata
import android.media.session.PlaybackState
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import com.springboard.launcher.systemui.SpringboardNotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@Stable
data class MediaSessionInfo(
    val packageName: String,
    val title: String?,
    val artist: String?,
    val albumArt: ImageBitmap?,
    val isPlaying: Boolean,
    val canControl: Boolean,
    val controller: MediaController?,
)

/**
 * Live, listener-driven state for everything the status bar and Control Center show:
 * time, battery, wifi, bluetooth, brightness, volume, active media session, cellular
 * signal and torch. Toggles reflect real system state; they are never mocked.
 */
class SystemStateRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val app = context.applicationContext
    private val contentResolver = app.contentResolver
    private val wifiManager = app.getSystemService(WifiManager::class.java)
    private val audioManager = app.getSystemService(AudioManager::class.java)
    private val cameraManager = app.getSystemService(CameraManager::class.java)
    private val mediaSessionManager = app.getSystemService(MediaSessionManager::class.java)
    private val telephonyManager = app.getSystemService(TelephonyManager::class.java)

    val timeText: StateFlow<String> = flow {
        while (true) {
            emit(formatTime(System.currentTimeMillis()))
            delay(1000)
        }
    }.stateIn(scope, SharingStarted.Eagerly, formatTime(System.currentTimeMillis()))

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _wifiEnabled = MutableStateFlow(false)
    val wifiEnabled: StateFlow<Boolean> = _wifiEnabled.asStateFlow()

    private val _wifiLevel = MutableStateFlow(0)
    val wifiLevel: StateFlow<Int> = _wifiLevel.asStateFlow()

    private val _bluetoothEnabled = MutableStateFlow(false)
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    private val _brightnessPercent = MutableStateFlow(50)
    val brightnessPercent: StateFlow<Int> = _brightnessPercent.asStateFlow()

    private val _volumePercent = MutableStateFlow(50)
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    val volumeMax: Int = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15

    private val _mediaSession = MutableStateFlow<MediaSessionInfo?>(null)
    val mediaSession: StateFlow<MediaSessionInfo?> = _mediaSession.asStateFlow()

    private val _signalLevel = MutableStateFlow(3)
    val signalLevel: StateFlow<Int> = _signalLevel.asStateFlow()

    private val _torchOn = MutableStateFlow(false)
    val torchOn: StateFlow<Boolean> = _torchOn.asStateFlow()

    private var torchCameraId: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mediaComponentName = ComponentName(app, SpringboardNotificationListener::class.java)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level >= 0 && scale > 0) _batteryLevel.value = level * 100 / scale
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            _isCharging.value =
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != WifiManager.WIFI_STATE_CHANGED_ACTION) return
            val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
            _wifiEnabled.value = state == WifiManager.WIFI_STATE_ENABLED
        }
    }

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            _bluetoothEnabled.value = state == BluetoothAdapter.STATE_ON
        }
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.VOLUME_CHANGED_ACTION) readVolume()
        }
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) _torchOn.value = enabled
        }
    }

    private val mediaListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        val top = controllers.orEmpty().firstOrNull { controller ->
            controller.mediaMetadata != null || controller.playbackState != null
        }
        _mediaSession.value = buildMediaInfo(top)
    }

    init {
        readInitialStates()
        registerReceivers()

        scope.launch {
            while (isActive) {
                readBrightness()
                readWifiLevel()
                readSignalIntoFlow()
                delay(1500)
            }
        }

        try {
            mediaSessionManager?.addOnActiveSessionsChangedListener(mediaListener, mediaComponentName, mainHandler)
            _mediaSession.value = buildMediaInfo(
                mediaSessionManager?.getActiveSessions(mediaComponentName)?.firstOrNull(),
            )
        } catch (t: Throwable) {
            _mediaSession.value = null
        }

        try {
            cameraManager?.registerTorchCallback(torchCallback, mainHandler)
        } catch (t: Throwable) {
            // torch simply stays unavailable on some devices
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun readInitialStates() {
        @Suppress("DEPRECATION")
        val batteryIntent = runCatching {
            app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        batteryReceiver.onReceive(null, batteryIntent)

        _wifiEnabled.value = readWifiEnabled()
        _bluetoothEnabled.value = readBluetoothEnabled()
        readWifiLevel()
        readBrightness()
        readVolume()
        readSignalIntoFlow()
    }

    private fun registerReceivers() {
        ContextCompat.registerReceiver(
            app,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
        ContextCompat.registerReceiver(
            app,
            wifiReceiver,
            IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION),
            ContextCompat.RECEIVER_EXPORTED,
        )
        ContextCompat.registerReceiver(
            app,
            btReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
        ContextCompat.registerReceiver(
            app,
            volumeReceiver,
            IntentFilter(AudioManager.VOLUME_CHANGED_ACTION),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private fun readWifiEnabled(): Boolean = try {
        wifiManager?.isWifiEnabled ?: false
    } catch (t: Throwable) {
        false
    }

    private fun readWifiLevel() {
        _wifiLevel.value = if (!readWifiEnabled()) {
            0
        } else {
            try {
                val info = wifiManager?.connectionInfo
                if (info == null || info.networkId == -1) 0 else rssiToLevel(info.rssi)
            } catch (t: Throwable) {
                0
            }
        }
    }

    private fun readBluetoothEnabled(): Boolean = try {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        adapter.isEnabled
    } catch (t: Throwable) {
        false
    }

    private fun readBrightness() {
        val value = try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        } catch (t: Throwable) {
            128
        }
        _brightnessPercent.value = (value * 100 / 255).coerceIn(0, 100)
    }

    private fun readVolume() {
        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: return
        if (max <= 0) return
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        _volumePercent.value = (current * 100 / max).coerceIn(0, 100)
    }

    private fun readSignalIntoFlow() {
        val level = readSignal()
        if (_signalLevel.value != level) _signalLevel.value = level
    }

    private fun readSignal(): Int {
        val level = try {
            val ss = telephonyManager?.signalStrength ?: return 3
            ss.level
        } catch (t: Throwable) {
            3
        }
        return if (level in 0..4) level else 3
    }

    private fun rssiToLevel(rssi: Int): Int = when {
        rssi >= -50 -> 4
        rssi >= -60 -> 3
        rssi >= -70 -> 2
        rssi >= -80 -> 1
        else -> 0
    }

    private fun buildMediaInfo(controller: MediaController?): MediaSessionInfo? {
        if (controller == null) return null
        val metadata = controller.mediaMetadata
        val state = controller.playbackState
        if (metadata == null && state == null) return null
        val isPlaying = state?.isActive == true && state.state == PlaybackState.STATE_PLAYING
        val info = MediaSessionInfo(
            packageName = controller.packageName,
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            albumArt = null,
            isPlaying = isPlaying,
            canControl = state != null,
            controller = controller,
        )
        if (metadata != null) {
            scope.launch(Dispatchers.IO) {
                val art = try {
                    metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)?.asImageBitmap()
                } catch (t: Throwable) {
                    null
                }
                if (art != null && _mediaSession.value?.packageName == controller.packageName) {
                    _mediaSession.value = _mediaSession.value?.copy(albumArt = art)
                }
            }
        }
        return info
    }

    // ======================= Actions =======================

    /** Writes system brightness. Only effective when Settings.System.canWrite is granted. */
    fun setBrightnessPercent(percent: Int): Boolean {
        val clamped = percent.coerceIn(5, 100)
        _brightnessPercent.value = clamped
        return try {
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, clamped * 255 / 100)
        } catch (t: Throwable) {
            false
        }
    }

    fun setVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _volumePercent.value = clamped
        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: return
        try {
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                ((clamped * max) / 100).coerceIn(0, max),
                0,
            )
        } catch (t: Throwable) {
            // ignored
        }
    }

    fun setWifiEnabled(enabled: Boolean, fallback: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            fallback()
            return
        }
        val changed = try {
            @Suppress("DEPRECATION")
            wifiManager?.isWifiEnabled = enabled
            true
        } catch (t: Throwable) {
            fallback()
            false
        }
        if (changed) _wifiEnabled.value = enabled
    }

    fun setBluetoothEnabled(enabled: Boolean, fallback: () -> Unit) {
        val adapter = try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (t: Throwable) {
            null
        }
        if (adapter == null) {
            fallback()
            return
        }
        val changed = try {
            if (enabled) {
                @Suppress("DEPRECATION")
                adapter.enable()
            } else {
                @Suppress("DEPRECATION")
                adapter.disable()
            }
            true
        } catch (t: Throwable) {
            fallback()
            false
        }
        if (changed) _bluetoothEnabled.value = enabled
    }

    fun toggleTorch() {
        val id = torchCameraId ?: findBackCamera()
        if (id == null) return
        torchCameraId = id
        try {
            val target = !_torchOn.value
            cameraManager?.setTorchMode(id, target)
            _torchOn.value = target
        } catch (t: Throwable) {
            // camera in use / unavailable
        }
    }

    private fun findBackCamera(): String? = try {
        cameraManager?.cameraIdList?.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    } catch (t: Throwable) {
        null
    }

    fun toggleMediaPlayPause() {
        val info = _mediaSession.value ?: return
        val transport = info.controller?.transportControls ?: return
        if (info.isPlaying) transport.pause() else transport.play()
    }

    private fun formatTime(now: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val is24 = DateFormat.is24HourFormat(app)
        val hour = if (is24) {
            hour24
        } else {
            val h = hour24 % 12
            if (h == 0) 12 else h
        }
        return String.format(Locale.getDefault(), "%d:%02d", hour, minute)
    }
}