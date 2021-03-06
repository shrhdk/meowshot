/*
 * Copyright (C) 2018 Hideki Shiro
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.shiro.meowshot

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import org.theta4j.plugin.*
import org.theta4j.plugin.ThetaIntent.KEY_CODE_SHUTTER
import org.theta4j.plugin.ThetaIntent.KEY_CODE_WIRELESS
import org.theta4j.webapi.CaptureMode
import org.theta4j.webapi.NetworkType
import org.theta4j.webapi.Options.*
import org.theta4j.webapi.Theta
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.schedule

class MainActivity : ThetaPluginActivity(), WebServer.Listener {
    companion object {
        val TAG = "MEOWSHOT"
    }

    private val RECORD_START_MARGIN = 500L
    private val RECORD_END_MARGIN = 600L
    private val RECORD_MAX_TIME = 15_000L

    private val executor = Executors.newSingleThreadExecutor()

    private val theta = Theta.createForPlugin()

    private var mBackupVolume: Int = 0

    private var mTimerTask: TimerTask? = null

    private var mWebServer: WebServer? = null

    private var mSoundManager: SoundManager? = null

    // Activity Events

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        executor.submit {
            theta.setOption(CAPTURE_MODE, CaptureMode.IMAGE)
        }
    }

    override fun onResume() {
        super.onResume()

        hideLED(LEDTarget.LED5)
        hideLED(LEDTarget.LED6)
        hideLED(LEDTarget.LED7)
        hideLED(LEDTarget.LED8)

        val soundFilePath = "${filesDir}${File.separator}mySound.wav"
        mSoundManager = SoundManager(applicationContext, R.raw.cat, soundFilePath)

        mWebServer = WebServer(applicationContext, this)
        mWebServer!!.start()

        executor.submit {
            val opts = theta.getOptions(SHUTTER_VOLUME, NETWORK_TYPE)

            mBackupVolume = opts.get(SHUTTER_VOLUME)!!

            if (opts.get(NETWORK_TYPE) == NetworkType.OFF) {
                setWLanMode(WLanMode.AP)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        mTimerTask?.cancel()
        mTimerTask = null

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
            return true
        } else if (keyCode == KEY_CODE_WIRELESS) {
            deleteSoundFile()
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEY_CODE_SHUTTER && !event.isCanceled) {
            play()
            return true
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
                mTimerTask?.cancel()
                mTimerTask = null

                // stop record
                mSoundManager!!.stopRecord(RECORD_END_MARGIN)
                hideLED(LEDTarget.LED7)
                forceRing(PresetSound.MOVIE_STOP)
            } else {
                // start record
                forceRing(PresetSound.MOVIE_START)
                Thread.sleep(RECORD_START_MARGIN) // For avoid recording sound effect
                showLED(LEDTarget.LED7)
                (getSystemService(Context.AUDIO_SERVICE) as AudioManager).run {
                    setParameters(ThetaAudio.RIC_MIC_DISABLE_B_FORMAT)
                }
                mSoundManager!!.startRecord()

                // start timeout task
                mTimerTask = Timer().schedule(RECORD_MAX_TIME) {
                    executor.submit {
                        startStopRecord()
                    }
                }
            }
        }
    }

    private fun deleteSoundFile() {
        executor.submit {
            mSoundManager!!.deleteFile()
            forceRing(PresetSound.SHUTTER_CLOSE)
        }
    }

    private fun takePicture() {
        executor.submit {
            if (mSoundManager!!.isRecording) {
                return@submit
            }

            theta.takePicture()
        }
    }

    private fun forceRing(sound: PresetSound) {
        theta.setOption(SHUTTER_VOLUME, 100)
        ring(sound)
        theta.setOption(SHUTTER_VOLUME, mBackupVolume)
    }
}
