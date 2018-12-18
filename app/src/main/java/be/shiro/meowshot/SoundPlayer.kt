package be.shiro.meowshot

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import org.theta4j.plugin.ThetaPluginActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService

class SoundPlayer(
    private val executor: ExecutorService,
    private val context: Context,
    resource: Int
) {
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

    init {
        executor.submit {
            val latch = CountDownLatch(1)
            soundPool.setOnLoadCompleteListener { _, _, _ ->
                latch.countDown()
            }
            defaultSoundID = soundPool.load(context, resource, 1)
            latch.await()
        }
    }

    private fun setSpeakerVolumeMax() {
        val audioManager = context.getSystemService(ThetaPluginActivity.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
    }

    fun load(path: String) {
        executor.submit {
            if (soundID != null) {
                soundPool.unload(soundID!!)
            }

            val latch = CountDownLatch(1)
            soundPool.setOnLoadCompleteListener { _, _, _ ->
                latch.countDown()
            }
            soundID = soundPool.load(path, 1)
            latch.await()
        }
    }

    fun unload() {
        executor.submit {
            if (soundID != null) {
                soundPool.unload(soundID!!)
                soundID = null
            }
        }
    }

    fun play() {
        executor.submit {
            setSpeakerVolumeMax()
            val id = soundID ?: defaultSoundID!!
            soundPool.play(id, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }
}
