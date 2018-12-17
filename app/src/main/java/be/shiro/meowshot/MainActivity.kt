package be.shiro.meowshot

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import org.theta4j.plugin.LEDTarget
import org.theta4j.plugin.ThetaIntent
import org.theta4j.plugin.ThetaPluginActivity
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : ThetaPluginActivity(), WebServer.Listener {
    private val executor = Executors.newSingleThreadExecutor()

    private var mWebServer: WebServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        hideLED(LEDTarget.LED4)
        hideLED(LEDTarget.LED5)
        hideLED(LEDTarget.LED6)
        hideLED(LEDTarget.LED7)
        hideLED(LEDTarget.LED8)

        mWebServer = WebServer(applicationContext, this)
        mWebServer!!.start()
    }

    override fun onPause() {
        super.onPause()

        mWebServer!!.stop()
        mWebServer = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == ThetaIntent.KEY_CODE_SHUTTER) {
            play()
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onMeowRequest() {
        play()
    }

    override fun onReleaseRequest() {
        takePicture()
    }

    private fun setVolumeMax() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
    }

    private fun play() {
        setVolumeMax()

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val soundPool = SoundPool.Builder()
            .setAudioAttributes(attributes)
            .setMaxStreams(1)
            .build()

        soundPool.setOnLoadCompleteListener { sp, id, status ->
            sp.play(id, 1.0f, 1.0f, 1, 0, 1.0f)
        }

        soundPool.load(this, R.raw.cat, 1)
    }

    private fun takePicture() {
        executor.submit {
            val reqBody = """{"name":"camera.takePicture"}""".toByteArray()
            val conn = URL("http://localhost:8080/osc/commands/execute").openConnection() as HttpURLConnection
            conn.apply {
                setFixedLengthStreamingMode(reqBody.size)
                doOutput = true
                requestMethod = "POST"
                addRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            conn.connect()
            conn.outputStream.use {
                it.write(reqBody)
            }
            if (conn.responseCode != 200) {
                Log.e(
                    "THETA_CAT",
                    "failed to execute takePicture command : ${conn.responseCode} ${conn.errorStream.reader().readText()}"
                )
            }
            conn.disconnect()
        }
    }
}
