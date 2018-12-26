/*
 * Copyright (C) 2018 theta4j project
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
import android.media.*
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import java.io.File

class SoundManager(
    private val context: Context,
    private val resource: Int,
    private val soundFilePath: String
) {
    private var mMediaPlayer: MediaPlayer? = null

    var isRecording: Boolean = false
        @Synchronized get

    private val bufferSize = android.media.AudioRecord.getMinBufferSize(
        WavFile.SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private var audioRecord: AudioRecord? = null

    private var wavFile: WavFile? = null

    @Synchronized
    fun play() {
        if (isRecording) {
            throw IllegalStateException("SoundManager is recording. Call stopRecord method before play method.")
        }

        stopPlay()

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVol, 0)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setLegacyStreamType(AudioManager.STREAM_RING)
            .build()

        mMediaPlayer = if (File(soundFilePath).exists()) {
            MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(soundFilePath)
                prepare()
            }
        } else {
            val sessionID = audioManager.generateAudioSessionId()
            MediaPlayer.create(context, resource, attributes, sessionID)
        }.apply {
            setVolume(1.0f, 1.0f)
            setOnCompletionListener {
                release()
                mMediaPlayer = null
            }
            start()
        }
    }

    @Synchronized
    fun stopPlay() {
        if (mMediaPlayer == null) {
            Log.d("MediaPlayer", "is null")
            return
        }
        if (mMediaPlayer!!.isPlaying) {
            mMediaPlayer!!.stop()
        }
        mMediaPlayer!!.release()
        mMediaPlayer = null
    }

    @Synchronized
    fun startRecord() {
        if (isRecording) {
            throw IllegalStateException("SoundManager is recording. Call stopRecord method before startRecord method.")
        }

        stopPlay()

        isRecording = true

        wavFile = WavFile(soundFilePath)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            WavFile.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        ).apply {
            if (NoiseSuppressor.isAvailable()) {
                Log.d(MainActivity.TAG, "Enable Noise Suppressor")
                NoiseSuppressor.create(audioSessionId)
            } else {
                Log.d(MainActivity.TAG, "Noise Suppressor is not available")
            }

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
                    synchronized(this@SoundManager) {
                        wavFile?.write(buf, sizeInShorts)
                    }
                }
            })
            positionNotificationPeriod = bufferSize / 2
            startRecording()
        }
    }

    @Synchronized
    fun stopRecord(endMargin: Long) {
        if (!isRecording) {
            return
        }

        stopPlay()

        audioRecord!!.run {
            setRecordPositionUpdateListener(null)
            stop()
            release()
            wavFile!!.cutEnd(endMargin)
            wavFile!!.close()
            wavFile = null
        }
        audioRecord = null
        isRecording = false
    }

    @Synchronized
    fun deleteFile() {
        stopPlay()

        if (isRecording) {
            throw IllegalStateException("SoundManager is recording. Call stopRecord method before deleteFile method.")
        }

        File(soundFilePath).run {
            if (exists()) {
                delete()
            }
        }
    }
}
