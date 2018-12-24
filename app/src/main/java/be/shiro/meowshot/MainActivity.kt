package be.shiro.meowshot

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import org.theta4j.plugin.LEDTarget
import org.theta4j.plugin.PresetSound
import org.theta4j.plugin.ThetaAudio
import org.theta4j.plugin.ThetaIntent.KEY_CODE_SHUTTER
import org.theta4j.plugin.ThetaIntent.KEY_CODE_WIRELESS
import org.theta4j.plugin.ThetaPluginActivity
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.Executors

class MainActivity : ThetaPluginActivity(), WebServer.Listener {
    companion object {
        val TAG = "MEOWSHOT"
    }

    private val RECORD_START_MARGIN = 500L
    private val RECORD_END_MARGIN = 600L
    private val RECORD_MAX_TIME = 15_000L

    private val executor = Executors.newSingleThreadExecutor()

    private val timer = Timer()

    private var mRecordTimeoutTask: TimerTask? = null

    private var mWebServer: WebServer? = null

    private var mSoundManager: SoundManager? = null

    // Activity Events

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

        val soundFilePath = "${filesDir}${File.separator}mySound.wav"
        mSoundManager = SoundManager(applicationContext, R.raw.cat, soundFilePath)

        mWebServer = WebServer(applicationContext, this)
        mWebServer!!.start()
    }

    override fun onPause() {
        super.onPause()

        mWebServer!!.stop()
        mWebServer = null

        executor.submit {
            mSoundManager!!.stopRecord(RECORD_END_MARGIN)
            mSoundManager = null
        }
    }

    // Key Events

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEY_CODE_SHUTTER && event.isLongPress) {
            startStopRecord()
        } else if (keyCode == KEY_CODE_WIRELESS) {
            deleteSoundFile()
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEY_CODE_SHUTTER && !event.isCanceled) {
            play()
        }
        return super.onKeyUp(keyCode, event)
    }

    // Web Server Events

    override fun onMeowRequest() {
        play()
    }

    override fun onReleaseRequest() {
        takePicture()
    }

    // Controlling Sound

    private fun play() {
        executor.submit {
            if (!mSoundManager!!.isRecording) {
                mSoundManager!!.play()
            }
        }
    }

    private fun startStopRecord() {
        executor.submit {
            if (mSoundManager!!.isRecording) {
                // stop timeout timer
                mRecordTimeoutTask?.cancel()

                // stop record
                mSoundManager!!.stopRecord(RECORD_END_MARGIN)
                hideLED(LEDTarget.LED7)
                ring(PresetSound.MOVIE_STOP)
            } else {
                // start record
                ring(PresetSound.MOVIE_START)
                Thread.sleep(RECORD_START_MARGIN) // For avoid recording sound effect
                showLED(LEDTarget.LED7)
                (getSystemService(Context.AUDIO_SERVICE) as AudioManager).run {
                    setParameters(ThetaAudio.RIC_MIC_DISABLE_B_FORMAT)
                }
                mSoundManager!!.startRecord()

                // start timeout timer
                mRecordTimeoutTask = object : TimerTask() {
                    override fun run() {
                        executor.submit {
                            startStopRecord()
                        }
                    }
                }
                timer.schedule(mRecordTimeoutTask!!, RECORD_MAX_TIME)
            }
        }
    }

    private fun deleteSoundFile() {
        executor.submit {
            mSoundManager!!.deleteFile()
            ring(PresetSound.SHUTTER_CLOSE)
        }
    }

    // Controlling Camera

    private fun takePicture() {
        executor.submit {
            if (mSoundManager!!.isRecording) {
                return@submit
            }

            val reqBody = """{"name":"camera.takePicture"}""".toByteArray()
            val conn = URL("http://127.0.0.1:8080/osc/commands/execute").openConnection() as HttpURLConnection
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
                    TAG,
                    "failed to execute takePicture command : ${conn.responseCode} ${conn.errorStream.reader().readText()}"
                )
            }
            conn.disconnect()
        }
    }
}
