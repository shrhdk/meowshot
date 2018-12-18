package be.shiro.meowshot

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.media.MediaRecorder.AudioEncoder
import android.media.MediaRecorder.AudioSource
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
import java.util.concurrent.Executors

class MainActivity : ThetaPluginActivity(), WebServer.Listener {
    private val RECORD_START_DELAY = 300L

    private val executor = Executors.newSingleThreadExecutor()

    private var mWebServer: WebServer? = null

    private var mSoundPlayer: SoundPlayer? = null

    private var mediaRecorder: MediaRecorder? = null

    private var soundFilePath: String? = null

    private var isRecording: Boolean = false // must be referenced from executor's thread.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        soundFilePath = "${filesDir}${File.separator}mySound.wav"
    }

    override fun onResume() {
        super.onResume()

        hideLED(LEDTarget.LED4)
        hideLED(LEDTarget.LED5)
        hideLED(LEDTarget.LED6)
        hideLED(LEDTarget.LED7)
        hideLED(LEDTarget.LED8)

        mSoundPlayer = SoundPlayer(executor, applicationContext, R.raw.cat)
        if (File(soundFilePath).exists()) {
            mSoundPlayer!!.load(soundFilePath!!)
        }

        mWebServer = WebServer(applicationContext, this)
        mWebServer!!.start()
    }

    override fun onPause() {
        super.onPause()

        mWebServer!!.stop()
        mWebServer = null

        mSoundPlayer = null

        executor.submit {
            // is Recording must be referenced from executor's thread.
            if (isRecording) {
                stopRecord()
            }
        }
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEY_CODE_SHUTTER && event.isLongPress) {
            executor.submit {
                // recording method must be invoked from executor's thread.
                if (isRecording) {
                    stopRecord()
                } else {
                    startRecord()
                }
            }
            return true // cancel onKeyUp event
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

    override fun onMeowRequest() {
        play()
    }

    override fun onReleaseRequest() {
        takePicture()
    }

    private fun play() {
        executor.submit {
            if (isRecording) {
                return@submit
            }

            mSoundPlayer!!.play()
        }
    }

    private fun deleteSoundFile() {
        val file = File(soundFilePath)
        if (file.exists()) {
            file.delete()
            mSoundPlayer!!.unload()
        }
        ring(PresetSound.SHUTTER_CLOSE)
    }

    private fun startRecord() {
        isRecording = true
        ring(PresetSound.MOVIE_START)
        Thread.sleep(RECORD_START_DELAY) // For avoid recording sound effect
        showLED(LEDTarget.LED7)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setParameters(ThetaAudio.RIC_MIC_DISABLE_B_FORMAT)

        mediaRecorder = MediaRecorder()
        mediaRecorder!!.setAudioSource(AudioSource.MIC)
        mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
        mediaRecorder!!.setAudioEncoder(AudioEncoder.DEFAULT)
        mediaRecorder!!.setOutputFile(soundFilePath!!)
        mediaRecorder!!.prepare()
        mediaRecorder!!.start()
    }

    private fun stopRecord() {
        mediaRecorder!!.stop();
        mediaRecorder!!.reset();
        mediaRecorder!!.release();
        mediaRecorder = null;

        hideLED(LEDTarget.LED7)
        ring(PresetSound.MOVIE_STOP)

        mSoundPlayer!!.load(soundFilePath!!)

        isRecording = false
    }

    private fun takePicture() {
        executor.submit {
            if (isRecording) {
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
                    "THETA_CAT",
                    "failed to execute takePicture command : ${conn.responseCode} ${conn.errorStream.reader().readText()}"
                )
            }
            conn.disconnect()
        }
    }
}
