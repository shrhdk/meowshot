package be.shiro.meowshot

import android.content.Context
import android.media.*
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

    private val bufferSize = android.media.AudioRecord.getMinBufferSize(
        WavFile.SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private var audioRecord: AudioRecord? = null

    private var wavFile: WavFile? = null

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
        if (isRecording) {
            throw IllegalStateException("SoundManager is recording. Call stopRecord method before startRecord method.")
        }

        isRecording = true

        wavFile = WavFile(soundFilePath)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            WavFile.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        ).apply {
            setRecordPositionUpdateListener(object : AudioRecord.OnRecordPositionUpdateListener {
                override fun onMarkerReached(recorder: AudioRecord?) {
                    // ignore
                }

                override fun onPeriodicNotification(recorder: AudioRecord) {
                    val buf = ShortArray(bufferSize / 2)
                    val sizeInShorts = recorder.read(buf, 0, buf.size)
                    if (sizeInShorts < 0) {
                        return
                    }
                    wavFile!!.write(buf, sizeInShorts)
                }
            })
            positionNotificationPeriod = bufferSize / 2;
            startRecording()
        }
    }

    @Synchronized
    fun stopRecord() {
        audioRecord?.run {
            setRecordPositionUpdateListener(null)
            stop()
            release()
            wavFile!!.close()
            load()
        }
        audioRecord = null;
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
