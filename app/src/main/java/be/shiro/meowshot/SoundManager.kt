package be.shiro.meowshot

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaRecorder
import android.media.SoundPool
import org.theta4j.plugin.ThetaPluginActivity
import java.io.File
import java.util.concurrent.CountDownLatch

class SoundManager(
    private val context: Context,
    private val resource: Int,
    private val soundFilePath: String
) {
    var isRecording: Boolean = false
        @Synchronized get

    private var defaultSoundID: Int? = null

    private var soundID: Int? = null

    private val soundPool = SoundPool.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setMaxStreams(1)
        .build()

    private var mediaRecorder: MediaRecorder? = null

    private fun setSpeakerVolumeMax() {
        val audioManager = context.getSystemService(ThetaPluginActivity.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
    }

    @Synchronized
    fun initialize() {
        loadDefault()
        if (File(soundFilePath).exists()) {
            load()
        }
    }

    @Synchronized
    fun play() {
        if (defaultSoundID == null) {
            throw IllegalStateException("Call initialize method first")
        }

        if (isRecording) {
            throw IllegalStateException("SoundManager is recording. Call stopRecord method before play method.")
        }

        setSpeakerVolumeMax()

        val id = soundID ?: defaultSoundID!!
        soundPool.play(id, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    @Synchronized
    fun startRecord() {
        isRecording = true
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
            setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
            setOutputFile(soundFilePath)
            prepare()
            start()
        }
    }

    @Synchronized
    fun stopRecord() {
        mediaRecorder?.run {
            stop()
            reset()
            release()
            load()
        }
        mediaRecorder = null;
        isRecording = false
    }

    @Synchronized
    fun deleteFile() {
        if (isRecording) {
            throw IllegalStateException("SoundManager is recording. Call stopRecord method before deleteFile method.")
        }

        if (soundID != null) {
            soundPool.unload(soundID!!)
            soundID = null
        }

        File(soundFilePath).run {
            if (exists()) {
                delete()
            }
        }
    }

    private fun loadDefault() {
        val latch = CountDownLatch(1)
        soundPool.setOnLoadCompleteListener { _, _, _ ->
            latch.countDown()
        }
        defaultSoundID = soundPool.load(context, resource, 1)
        latch.await()
    }

    private fun load() {
        if (soundID != null) {
            soundPool.unload(soundID!!)
        }

        val latch = CountDownLatch(1)
        soundPool.setOnLoadCompleteListener { _, _, _ ->
            latch.countDown()
        }
        soundID = soundPool.load(soundFilePath, 1)
        latch.await()
    }
}
