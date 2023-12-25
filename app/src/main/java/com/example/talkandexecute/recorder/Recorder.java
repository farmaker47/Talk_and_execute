package com.example.talkandexecute.recorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.talkandexecute.utils.WaveUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

public class Recorder {
    public static final String TAG = "Recorder";

    private final Context mContext;
    private final AtomicBoolean mInProgress = new AtomicBoolean(false);

    private String mWavFilePath = null;
    private Thread mExecutorThread = null;

    public Recorder(Context context) {
        mContext = context;
    }

    public void setFilePath(String wavFile) {
        mWavFilePath = wavFile;
    }

    public void start() {
        if (mInProgress.get()) {
            Log.d(TAG, "Recording is in progress...");
            return;
        }

        mExecutorThread = new Thread(() -> {
            mInProgress.set(true);
            threadFunction();
            mInProgress.set(false);
        });
        mExecutorThread.start();
    }

    public void stop() {
        mInProgress.set(false);
        try {
            if (mExecutorThread != null) {
                mExecutorThread.join();
                mExecutorThread = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void threadFunction() {
        try {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "AudioRecord permission is not granted");
                return;
            }

            int channels = 1;
            int bytesPerSample = 2;
            int sampleRateInHz = 16000;
            int channelConfig = AudioFormat.CHANNEL_IN_MONO; // as per channels
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT; // as per bytesPerSample
            int audioSource = MediaRecorder.AudioSource.MIC;

            int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            AudioRecord audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSize);
            audioRecord.startRecording();

            int bufferSize1Sec = sampleRateInHz * bytesPerSample * channels;
            int bufferSize30Sec = bufferSize1Sec * 30;
            ByteBuffer buffer30Sec = ByteBuffer.allocateDirect(bufferSize30Sec);
            ByteBuffer bufferRealtime = ByteBuffer.allocateDirect(bufferSize1Sec * 5);

            int timer = 0;
            int totalBytesRead = 0;
            byte[] audioData = new byte[bufferSize];
            while (mInProgress.get() && (totalBytesRead < bufferSize30Sec)) {
                int bytesRead = audioRecord.read(audioData, 0, bufferSize);
                if (bytesRead > 0) {
                    buffer30Sec.put(audioData, 0, bytesRead);
                    bufferRealtime.put(audioData, 0, bytesRead);
                } else {
                    Log.d(TAG, "AudioRecord error, bytes read: " + bytesRead);
                    break;
                }

                // Update timer after every second
                totalBytesRead = totalBytesRead + bytesRead;
                int timer_tmp = totalBytesRead / bufferSize1Sec;
                if (timer != timer_tmp) {
                    timer = timer_tmp;

                    // Transcribe realtime buffer after every 3 seconds
                    if (timer % 3 == 0) {
                        // Flip the buffer for reading
                        bufferRealtime.flip();
                        bufferRealtime.order(ByteOrder.nativeOrder());

                        // Create a sample array to hold the converted data
                        float[] samples = new float[bufferRealtime.remaining() / 2];

                        // Convert ByteBuffer to short array
                        for (int i = 0; i < samples.length; i++) {
                            samples[i] = (float) (bufferRealtime.getShort() / 32768.0);
                        }

                        // Reset the ByteBuffer for writing again
                        bufferRealtime.clear();
                    }
                }
            }

            audioRecord.stop();
            audioRecord.release();

            // Save 30 seconds of recording buffer in wav file
            WaveUtil.createWaveFile(mWavFilePath, buffer30Sec.array(), sampleRateInHz, channels, bytesPerSample);
            Log.d(TAG, "Recorded file: " + mWavFilePath);
        } catch (Exception e) {
            Log.e(TAG, "Error...", e);
        }
    }
}
