/*
 * Copyright (c) 2025 Vrushal
 *
 * This file is part of Zon (forked from Tomato) - a minimalist pomodoro timer for Android.
 *
 * Zon is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Zon is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Zon.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.vrn7712.pomodoro.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vrn7712.pomodoro.R
import org.vrn7712.pomodoro.ZonApplication
import org.vrn7712.pomodoro.ui.timerScreen.viewModel.TimerMode
import org.vrn7712.pomodoro.utils.millisecondsToStr
import kotlin.text.Typography.middleDot
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import org.vrn7712.pomodoro.ui.widget.TimerGlanceWidget
import org.vrn7712.pomodoro.ui.widget.FocusStatsGlanceWidget
import org.vrn7712.pomodoro.ui.widget.FocusHistoryGlanceWidget
import androidx.glance.appwidget.updateAll


class TimerService : Service() {
    
    companion object {
        /** Intent extra key for the seek position (0.0 to 1.0) */
        const val EXTRA_SEEK_PROGRESS = "extra_seek_progress"
    }
    private val appContainer by lazy {
        (application as ZonApplication).container
    }

    private val stateRepository by lazy { appContainer.stateRepository }
    private val statRepository by lazy { appContainer.appStatRepository }
    private val taskRepository by lazy { appContainer.taskRepository }
    private val notificationManager by lazy { appContainer.notificationManager }
    private val notificationManagerService by lazy { appContainer.notificationManagerService }
    private val notificationBuilder by lazy { appContainer.notificationBuilder }
    private val _timerState by lazy { stateRepository.timerState }
    private val _settingsState by lazy { stateRepository.settingsState }
    private val _time by lazy { appContainer.time }

    /**
     * Remaining time
     */
    private var time: Long
        get() = _time.value
        set(value) = _time.update { value }

    private var cycles = 0
    private var startTime = 0L
    private var pauseTime = 0L
    private var pauseDuration = 0L

    private var lastSavedDuration = 0L

    private val timerStateSnapshot by lazy { stateRepository.timerStateSnapshot }

    private val saveLock = Mutex()
    private var job = SupervisorJob()
    private val timerScope = CoroutineScope(Dispatchers.IO + job)
    private val skipScope = CoroutineScope(Dispatchers.IO + job)

    private var autoAlarmStopScope: Job? = null

    private var alarm: MediaPlayer? = null
    private val vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }


    private var musicPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null


    private val cs get() = stateRepository.colorScheme

    private var notificationStyle = NotificationCompat.ProgressStyle()
    
    /**
     * Parses a hex color string (e.g., "#FF4E9A62") to an Int color value.
     * Returns 0 if the string is invalid or cannot be parsed.
     */
    private fun parseHexColor(hexColor: String): Int {
        return try {
            android.graphics.Color.parseColor(hexColor)
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        stateRepository.timerState.update { it.copy(serviceRunning = true) }
        
        // Initialize style immediately
        updateProgressSegments()

        try {
            alarm = initializeMediaPlayer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            initializeMusicPlayer()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Observe settings changes to update widget colors/mode immediately
        timerScope.launch {
            _settingsState.collect {
                val total = _timerState.value.totalTime.toFloat().coerceAtLeast(1f)
                val current = time.toFloat()
                val prog = (1f - (current / total)).coerceIn(0f, 1f)
                updateWidget(millisecondsToStr(time), _timerState.value.timerRunning, prog)
            }
        }
    }

    override fun onDestroy() {
        stateRepository.timerState.update { it.copy(serviceRunning = false) }
        
        // Cancel coroutines first
        job.cancel()
        
        // Save time to database in a non-blocking way
        // Using GlobalScope here because the service is being destroyed
        // and we need the save to complete even after service lifecycle ends
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                saveTimeToDb()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Synchronous cleanup that doesn't require coroutines
        lastSavedDuration = 0
        setDoNotDisturb(false)
        notificationManager.cancel(1)
        
        // Release media resources synchronously (these are fast operations)
        try {
            alarm?.release()
            mediaSession?.release()
            musicPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.TOGGLE.toString() -> {
                startForegroundService()
                toggleTimer()
            }

            Actions.RESET.toString() -> {
                if (_timerState.value.timerRunning) toggleTimer()
                skipScope.launch {
                    resetTimer()
                    stopForegroundService()
                }
            }

            Actions.UNDO_RESET.toString() -> undoReset()

            Actions.SKIP.toString() -> skipScope.launch { skipTimer(true) }

            Actions.STOP_ALARM.toString() -> stopAlarm()



            Actions.UPDATE_ALARM_TONE.toString() -> updateAlarmTone()

            Actions.TOGGLE_MUSIC.toString() -> toggleMusic()

            Actions.SKIP_MUSIC_NEXT.toString() -> skipMusic(true)

            Actions.SKIP_MUSIC_PREV.toString() -> skipMusic(false)

            Actions.RELOAD_MUSIC.toString() -> reloadMusicPlayer()

            Actions.SEEK_MUSIC.toString() -> {
                val progress = intent?.getFloatExtra(EXTRA_SEEK_PROGRESS, 0f) ?: 0f
                seekMusic(progress)
            }

            Actions.REFRESH_WIDGET.toString() -> {
                // Trigger widget update with current timer state to sync colors
                val state = _timerState.value
                updateWidget(state.timeStr, state.timerRunning, 0f)
            }

            Actions.REFRESH_STATS_WIDGET.toString() -> {
                updateFocusStatsWidget()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun toggleTimer() {
        updateProgressSegments()

        if (_timerState.value.timerRunning) {
            setDoNotDisturb(false)
            notificationBuilder.clearActions().addTimerActions(
                this, R.drawable.play, getString(R.string.start)
            )
            showTimerNotification(time.toInt(), paused = true)
            _timerState.update { currentState ->
                currentState.copy(timerRunning = false)
            }
            // Passing default progress 0f or actual calc because we need it
            // Safe to pass 0f or calculation here.
            val total = _timerState.value.totalTime.toFloat().coerceAtLeast(1f)
            val current = time.toFloat()
            val prog = (1f - (current / total)).coerceIn(0f, 1f)
            updateWidget(millisecondsToStr(time), false, prog)
            pauseTime = SystemClock.elapsedRealtime()
        } else {
            if (_timerState.value.timerMode == TimerMode.FOCUS) setDoNotDisturb(true)
            else setDoNotDisturb(false)
            notificationBuilder.clearActions().addTimerActions(
                this, R.drawable.pause, getString(R.string.stop)
            )
            _timerState.update { it.copy(timerRunning = true) }
            val total = _timerState.value.totalTime.toFloat().coerceAtLeast(1f)
            val current = time.toFloat()
            val prog = (1f - (current / total)).coerceIn(0f, 1f)
            updateWidget(millisecondsToStr(time), true, prog)
            
            if (pauseTime != 0L) pauseDuration += SystemClock.elapsedRealtime() - pauseTime

            var iterations = -1

            timerScope.launch {
                while (true) {
                    if (!_timerState.value.timerRunning) break
                    if (startTime == 0L) startTime = SystemClock.elapsedRealtime()

                    val settingsState = _settingsState.value
                    time = when (_timerState.value.timerMode) {
                        TimerMode.FOCUS -> settingsState.focusTime - (SystemClock.elapsedRealtime() - startTime - pauseDuration)

                        TimerMode.SHORT_BREAK -> settingsState.shortBreakTime - (SystemClock.elapsedRealtime() - startTime - pauseDuration)

                        else -> settingsState.longBreakTime - (SystemClock.elapsedRealtime() - startTime - pauseDuration)
                    }

                    iterations =
                        (iterations + 1) % stateRepository.timerFrequency.toInt().coerceAtLeast(1)

                    if (iterations == 0) showTimerNotification(time.toInt())

                    if (time < 0) {
                        skipTimer()
                        _timerState.update { currentState ->
                            currentState.copy(timerRunning = false)
                        }
                        break
                    } else {
                        _timerState.update { currentState ->
                            currentState.copy(
                                timeStr = millisecondsToStr(time)
                            )
                        }
                        val totalTimeVal = _timerState.value.totalTime

                        if (totalTimeVal - time < lastSavedDuration)
                            lastSavedDuration =
                                0 // Sanity check, prevents bugs if service is force closed
                        if (totalTimeVal - time - lastSavedDuration > 60000)
                            saveTimeToDb()
                    }
                    
                    val totalDuration = _timerState.value.totalTime.toFloat().coerceAtLeast(1f)
                    val currentDuration = time.toFloat()
                    val progress = (1f - (currentDuration / totalDuration)).coerceIn(0f, 1f)
                    updateWidget(millisecondsToStr(time), true, progress)

                    delay((1000f / stateRepository.timerFrequency).toLong())
                }
            }
        }
    }

    @SuppressLint(
        "MissingPermission",
        "StringFormatInvalid"
    ) // We check for the permission when pressing the Play button in the UI
    fun showTimerNotification(
        remainingTime: Int, paused: Boolean = false, complete: Boolean = false
    ) {
        val settingsState = _settingsState.value

        if (complete) notificationBuilder.clearActions().addStopAlarmAction(this)

        val totalTime = when (_timerState.value.timerMode) {
            TimerMode.FOCUS -> settingsState.focusTime.toInt()
            TimerMode.SHORT_BREAK -> settingsState.shortBreakTime.toInt()
            else -> settingsState.longBreakTime.toInt()
        }

        val currentTimer = when (_timerState.value.timerMode) {
            TimerMode.FOCUS -> getString(R.string.focus)
            TimerMode.SHORT_BREAK -> getString(R.string.short_break)
            else -> getString(R.string.long_break)
        }

        val nextTimer = when (_timerState.value.nextTimerMode) {
            TimerMode.FOCUS -> getString(R.string.focus)
            TimerMode.SHORT_BREAK -> getString(R.string.short_break)
            else -> getString(R.string.long_break)
        }

        val remainingTimeString = if ((remainingTime.toFloat() / 60000f) < 1.0f) "< 1"
        else (remainingTime.toFloat() / 60000f).toInt()

        notificationManager.notify(
            1,
            notificationBuilder
                .setContentTitle(
                    if (!complete) {
                        "$currentTimer  $middleDot  ${
                            getString(R.string.min_remaining_notification, remainingTimeString)
                        }" + if (paused) "  $middleDot  ${getString(R.string.paused)}" else ""
                    } else "$currentTimer $middleDot ${getString(R.string.completed)}"
                )
                .setContentText(
                    getString(
                        R.string.up_next_notification,
                        nextTimer,
                        _timerState.value.nextTimeStr
                    )
                )
                .setStyle(
                    notificationStyle
                        .setProgress( // Set the current progress by filling the previous intervals and part of the current interval
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && !settingsState.singleProgressBar) {
                                (totalTime - remainingTime) + ((cycles + 1) / 2) * settingsState.focusTime.toInt() + (cycles / 2) * settingsState.shortBreakTime.toInt()
                            } else (totalTime - remainingTime)
                        )
                )
                .setWhen(System.currentTimeMillis() + remainingTime) // Sets the Live Activity/Now Bar chip time
                .setShortCriticalText(millisecondsToStr(time.coerceAtLeast(0)))
                .build()
        )

        if (complete) {
            startAlarm()
            _timerState.update { currentState ->
                currentState.copy(alarmRinging = true)
            }
        }
    }

    private fun updateProgressSegments() {
        val settingsState = _settingsState.value
        notificationStyle = NotificationCompat.ProgressStyle()
            .also {
                // Add all the Focus, Short break and long break intervals in order
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && !settingsState.singleProgressBar) {
                    // Android 16 and later supports live updates
                    // Set progress bar sections if on Baklava or later
                    for (i in 0..<settingsState.sessionLength * 2) {
                        if (i % 2 == 0) it.addProgressSegment(
                            NotificationCompat.ProgressStyle.Segment(
                                settingsState.focusTime.toInt()
                            )
                                .setColor(cs.primary.toArgb())
                        )
                        else if (i != (settingsState.sessionLength * 2 - 1)) it.addProgressSegment(
                            NotificationCompat.ProgressStyle.Segment(
                                settingsState.shortBreakTime.toInt()
                            ).setColor(cs.tertiary.toArgb())
                        )
                        else it.addProgressSegment(
                            NotificationCompat.ProgressStyle.Segment(
                                settingsState.longBreakTime.toInt()
                            ).setColor(cs.tertiary.toArgb())
                        )
                    }
                } else {
                    it.addProgressSegment(
                        NotificationCompat.ProgressStyle.Segment(
                            when (_timerState.value.timerMode) {
                                TimerMode.FOCUS -> settingsState.focusTime.toInt()
                                TimerMode.SHORT_BREAK -> settingsState.shortBreakTime.toInt()
                                else -> settingsState.longBreakTime.toInt()
                            }
                        )
                    )
                }
            }
    }

    private suspend fun resetTimer() {
        val settingsState = _settingsState.value

        timerStateSnapshot.save(
            lastSavedDuration,
            time,
            cycles,
            startTime,
            pauseTime,
            pauseDuration,
            _timerState.value
        )

        saveTimeToDb()
        lastSavedDuration = 0
        time = settingsState.focusTime
        cycles = 0
        startTime = 0L
        pauseTime = 0L
        pauseDuration = 0L

        _timerState.update { currentState ->
            currentState.copy(
                timerMode = TimerMode.FOCUS,
                timeStr = millisecondsToStr(time),
                totalTime = time,
                nextTimerMode = if (settingsState.sessionLength > 1) TimerMode.SHORT_BREAK else TimerMode.LONG_BREAK,
                nextTimeStr = millisecondsToStr(if (settingsState.sessionLength > 1) settingsState.shortBreakTime else settingsState.longBreakTime),
                currentFocusCount = 1,
                totalFocusCount = settingsState.sessionLength
            )
        }

        updateProgressSegments()
        updateWidget(millisecondsToStr(time), false, 0f)
    }

    private fun undoReset() {
        lastSavedDuration = timerStateSnapshot.lastSavedDuration
        time = timerStateSnapshot.time
        cycles = timerStateSnapshot.cycles
        startTime = timerStateSnapshot.startTime
        pauseTime = timerStateSnapshot.pauseTime
        pauseDuration = timerStateSnapshot.pauseDuration
        _timerState.update { timerStateSnapshot.timerState }
    }

    private suspend fun skipTimer(fromButton: Boolean = false) {
        val settingsState = _settingsState.value
        saveTimeToDb()
        updateProgressSegments()
        showTimerNotification(0, paused = true, complete = !fromButton)
        lastSavedDuration = 0
        startTime = 0L
        pauseTime = 0L
        pauseDuration = 0L

        cycles = (cycles + 1) % (settingsState.sessionLength * 2)

        if (cycles % 2 == 0) {
            if (_timerState.value.timerRunning) setDoNotDisturb(true)
            time = settingsState.focusTime
            _timerState.update { currentState ->
                currentState.copy(
                    timerMode = TimerMode.FOCUS,
                    timeStr = millisecondsToStr(time),
                    totalTime = time,
                    nextTimerMode = if (cycles == (settingsState.sessionLength - 1) * 2) TimerMode.LONG_BREAK else TimerMode.SHORT_BREAK,
                    nextTimeStr = if (cycles == (settingsState.sessionLength - 1) * 2) millisecondsToStr(
                        settingsState.longBreakTime
                    ) else millisecondsToStr(
                        settingsState.shortBreakTime
                    ),
                    currentFocusCount = cycles / 2 + 1,
                    totalFocusCount = settingsState.sessionLength
                )
            }
        } else {
            if (_timerState.value.timerRunning) setDoNotDisturb(false)
            val long = cycles == (settingsState.sessionLength * 2) - 1
            time = if (long) settingsState.longBreakTime else settingsState.shortBreakTime

            _timerState.update { currentState ->
                currentState.copy(
                    timerMode = if (long) TimerMode.LONG_BREAK else TimerMode.SHORT_BREAK,
                    timeStr = millisecondsToStr(time),
                    totalTime = time,
                    nextTimerMode = TimerMode.FOCUS,
                    nextTimeStr = millisecondsToStr(settingsState.focusTime)
                )
            }
        }

        updateProgressSegments()
        updateWidget(millisecondsToStr(time), _timerState.value.timerRunning, 0f)
    }

    fun startAlarm() {
        val settingsState = _settingsState.value
        if (settingsState.alarmEnabled) alarm?.start()

        appContainer.activityTurnScreenOn(true)

        autoAlarmStopScope = CoroutineScope(Dispatchers.IO).launch {
            delay(1 * 60 * 1000)
            stopAlarm(fromAutoStop = true)
        }

        if (settingsState.vibrateEnabled) {
            if (!vibrator.hasVibrator()) {
                return
            }
            val vibrationPattern = longArrayOf(0, 1000, 1000, 1000)
            val repeat = 2
            val effect = VibrationEffect.createWaveform(vibrationPattern, repeat)
            vibrator.vibrate(effect)
        }
    }

    /**
     * Stops ringing the alarm and vibration, and performs related necessary actions
     *
     * @param fromAutoStop Whether the function was triggered automatically by the program instead of
     * intentionally by the user
     */
    fun stopAlarm(fromAutoStop: Boolean = false) {
        val settingsState = _settingsState.value
        autoAlarmStopScope?.cancel()

        if (settingsState.alarmEnabled) {
            alarm?.pause()
            alarm?.seekTo(0)
        }

        if (settingsState.vibrateEnabled) {
            vibrator.cancel()
        }

        appContainer.activityTurnScreenOn(false)

        _timerState.update { currentState ->
            currentState.copy(alarmRinging = false)
        }
        notificationBuilder.clearActions().addTimerActions(
            this, R.drawable.play,
            getString(R.string.start_next)
        )
        showTimerNotification(
            when (_timerState.value.timerMode) {
                TimerMode.FOCUS -> settingsState.focusTime.toInt()
                TimerMode.SHORT_BREAK -> settingsState.shortBreakTime.toInt()
                else -> settingsState.longBreakTime.toInt()
            }, paused = true, complete = false
        )

        if (settingsState.autostartNextSession && !fromAutoStop)  // auto start next session
            toggleTimer()
    }

    private fun initializeMediaPlayer(): MediaPlayer? {
        val settingsState = _settingsState.value
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(
                        if (settingsState.mediaVolumeForAlarm) AudioAttributes.USAGE_MEDIA
                        else AudioAttributes.USAGE_ALARM
                    )
                    .build()
                )
                settingsState.alarmSoundUri?.let {
                    setDataSource(applicationContext, it)
                    prepareAsync()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun setDoNotDisturb(doNotDisturb: Boolean) {
        if (_settingsState.value.dndEnabled && notificationManagerService.isNotificationPolicyAccessGranted()) {
            if (doNotDisturb) {
                notificationManagerService.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            } else notificationManagerService.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

    private fun updateAlarmTone() {
        alarm?.release()
        alarm = initializeMediaPlayer()
    }

    private var musicProgressJob: Job? = null
    
    private fun initializeMusicPlayer() {
        if (musicPlayer != null) return
        try {
            musicPlayer = ExoPlayer.Builder(applicationContext).build().apply {
                val settingsState = _settingsState.value
                
                // Use custom music if set, otherwise load both built-in tracks
                val mediaItems = if (!settingsState.musicSoundUri.isNullOrEmpty()) {
                    // User has selected custom music
                    listOf(MediaItem.fromUri(settingsState.musicSoundUri))
                } else {
                    // Load all built-in tracks so next/prev buttons work
                    listOf(
                        MediaItem.fromUri("android.resource://${packageName}/${R.raw.cozy_lofi}"),
                        MediaItem.fromUri("android.resource://${packageName}/${R.raw.study_music}")
                    )
                }
                
                setMediaItems(mediaItems)
                
                // Start on the selected default track
                if (settingsState.musicSoundUri.isNullOrEmpty()) {
                    when (settingsState.defaultMusicTrack) {
                        "study_music" -> seekTo(1, 0)  // Study Music is at index 1
                        else -> seekTo(0, 0) // cozy_lofi is at index 0 (default)
                    }
                }
                
                prepare()
                repeatMode = Player.REPEAT_MODE_ALL
                
                try {
                    mediaSession = MediaSession.Builder(applicationContext, this).build()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
    
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        // If custom music fails to play, fall back to default tracks
                        if (!settingsState.musicSoundUri.isNullOrEmpty()) {
                            _settingsState.update { it.copy(musicSoundUri = null) }
                            // Reload with default tracks
                            setMediaItems(
                                listOf(
                                    MediaItem.fromUri("android.resource://${packageName}/${R.raw.cozy_lofi}"),
                                    MediaItem.fromUri("android.resource://${packageName}/${R.raw.study_music}")
                                )
                            )
                            prepare()
                        }
                    }
                    
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _timerState.update { it.copy(isMusicPlaying = isPlaying) }
                        // Update progress immediately when play state changes
                        updateMusicProgress()
                    }
    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _timerState.update {
                            it.copy(isMusicLoading = playbackState == Player.STATE_BUFFERING)
                        }
                        // When ready, update the duration
                        if (playbackState == Player.STATE_READY) {
                            updateMusicProgress()
                        }
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Start progress update coroutine
        startMusicProgressUpdates()
    }
    
    private fun startMusicProgressUpdates() {
        // Cancel any existing job
        musicProgressJob?.cancel()
        
        // Start a new coroutine to update music progress periodically
        // IMPORTANT: ExoPlayer must be accessed from Main thread
        musicProgressJob = CoroutineScope(Dispatchers.Main + job).launch {
            while(true) {
                // Only update progress when music is actually playing
                // This saves CPU cycles when paused
                if (musicPlayer?.isPlaying == true) {
                    updateMusicProgress()
                }
                delay(250) // Update 4 times a second for smooth progress
            }
        }
    }
    
    private fun updateMusicProgress() {
        try {
            musicPlayer?.let { player ->
                val duration = player.duration
                val position = player.currentPosition
                
                // Check if duration is valid
                // C.TIME_UNSET is Long.MIN_VALUE + 1 = -9223372036854775807L
                if (duration > 0) {
                    val progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    val currentMs = position.coerceAtLeast(0L)
                    val durationMs = duration.coerceAtLeast(0L)
                    
                    _timerState.update { 
                        it.copy(
                            musicProgress = progress,
                            musicCurrentTime = formatMusicTime(currentMs),
                            musicDuration = formatMusicTime(durationMs)
                        ) 
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Format time for music player (mm:ss format)
    private fun formatMusicTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun toggleMusic() {
        if (musicPlayer == null) initializeMusicPlayer()
        musicPlayer?.let { player ->
             try {
                if (player.isPlaying) player.pause() else player.play()
             } catch (e: Exception) {
                 e.printStackTrace()
             }
        }
    }

    private fun skipMusic(next: Boolean) {
        musicPlayer?.let { player ->
            if (next) {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem() 
                } else {
                    // If single item or last item, restart
                    player.seekTo(0)
                }
            } else {
                 if (player.currentPosition > 3000) {
                     // If played more than 3 sec, restart song
                     player.seekTo(0)
                 } else {
                     if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() 
                     else player.seekTo(0) // Restart if no previous
                 }
            }
        }
    }

    private fun reloadMusicPlayer() {
        // Cancel existing progress updates
        musicProgressJob?.cancel()
        musicProgressJob = null
        
        // Release existing player
        musicPlayer?.let { player ->
            player.stop()
            player.release()
        }
        musicPlayer = null
        mediaSession?.release()
        mediaSession = null
        
        // Reinitialize with new settings
        initializeMusicPlayer()
    }
    
    /**
     * Seek to a specific position in the currently playing music.
     * @param progress Position to seek to, as a fraction from 0.0 (start) to 1.0 (end)
     */
    private fun seekMusic(progress: Float) {
        musicPlayer?.let { player ->
            try {
                val duration = player.duration
                // Validate duration is available
                if (duration > 0) {
                    val targetPosition = (progress.coerceIn(0f, 1f) * duration).toLong()
                    player.seekTo(targetPosition)
                    // Update progress immediately for responsive UI
                    updateMusicProgress()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    suspend fun saveTimeToDb() {
        saveLock.withLock {
            val elapsedTime = _timerState.value.totalTime - time
            when (_timerState.value.timerMode) {
                TimerMode.FOCUS -> statRepository.addFocusTime(
                    (elapsedTime - lastSavedDuration).coerceAtLeast(0L)
                )

                else -> statRepository.addBreakTime(
                    (elapsedTime - lastSavedDuration).coerceAtLeast(0L)
                )
            }
            lastSavedDuration = elapsedTime
        }
    }

    private fun startForegroundService() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    1,
                    notificationBuilder.build(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(1, notificationBuilder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopForegroundService() {
        try {
            notificationManager.cancel(1)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateWidget(timeStr: String, isRunning: Boolean, progress: Float) {
        val settings = _settingsState.value
        // Parse hex color strings to Int for widget preferences
        // Uses android.graphics.Color.parseColor which handles "#AARRGGBB" format
        val primaryColor = settings.primaryColor?.let { parseHexColor(it) } ?: 0
        val surfaceColor = settings.surfaceColor?.let { parseHexColor(it) } ?: 0
        val onSurfaceColor = settings.onSurfaceColor?.let { parseHexColor(it) } ?: 0
        val surfaceVariantColor = settings.surfaceVariantColor?.let { parseHexColor(it) } ?: 0
        val secondaryContainerColor = settings.secondaryContainerColor?.let { parseHexColor(it) } ?: 0
        val onSecondaryContainerColor = settings.onSecondaryContainerColor?.let { parseHexColor(it) } ?: 0
        val timerMode = _timerState.value.timerMode.name

        // Use timerScope tied to service lifecycle instead of orphaned CoroutineScope
        timerScope.launch {
            try {
                val context: Context = applicationContext
                val manager = GlanceAppWidgetManager(context)
                val widget = TimerGlanceWidget()
                val glanceIds = manager.getGlanceIds(widget.javaClass)
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[stringPreferencesKey("time")] = timeStr
                        prefs[booleanPreferencesKey("isRunning")] = isRunning
                        prefs[androidx.datastore.preferences.core.floatPreferencesKey("progress")] = progress
                        prefs[androidx.datastore.preferences.core.intPreferencesKey("activeColor")] = primaryColor
                        prefs[androidx.datastore.preferences.core.intPreferencesKey("surfaceColor")] = surfaceColor
                        prefs[androidx.datastore.preferences.core.intPreferencesKey("onSurfaceColor")] = onSurfaceColor
                        prefs[androidx.datastore.preferences.core.intPreferencesKey("inactiveColor")] = surfaceVariantColor
                        prefs[androidx.datastore.preferences.core.intPreferencesKey("secondaryContainer")] = secondaryContainerColor
                        prefs[androidx.datastore.preferences.core.intPreferencesKey("onSecondaryContainer")] = onSecondaryContainerColor
                        prefs[stringPreferencesKey("timerMode")] = timerMode
                    }
                    widget.update(context, glanceId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateFocusStatsWidget() {
        // Use timerScope tied to service lifecycle instead of orphaned CoroutineScope
        timerScope.launch {
            try {
                val context: Context = applicationContext
                
                // Update Focus Stats Widget (uses direct repository access)
                FocusStatsGlanceWidget().updateAll(context)
                
                // Update Focus History Widget (uses direct repository access)
                FocusHistoryGlanceWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    enum class Actions {
        TOGGLE, SKIP, RESET, UNDO_RESET, STOP_ALARM, UPDATE_ALARM_TONE,
        TOGGLE_MUSIC, SKIP_MUSIC_NEXT, SKIP_MUSIC_PREV, SEEK_MUSIC, RELOAD_MUSIC, 
        REFRESH_WIDGET, REFRESH_STATS_WIDGET
    }
}
