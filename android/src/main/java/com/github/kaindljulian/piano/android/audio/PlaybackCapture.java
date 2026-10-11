package com.github.kaindljulian.piano.android.audio;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.os.Build;

import com.github.kaindljulian.piano.audio.AudioCapture;

/**
 * Captures audio using the AudioPlaybackCapture API (Android 10+).
 * Needs the RECORD_AUDIO permission and a {@link MediaProjection} from {@link PlaybackCaptureRequest}.
 */
@TargetApi(Build.VERSION_CODES.Q)
public class PlaybackCapture implements AudioCapture {
    public static final int DEFAULT_SAMPLE_RATE = 44100;
    public static final int DEFAULT_CHANNEL_COUNT = 2;

    private final AudioRecord record;
    private final int sampleRate;
    private final int channelCount;
    private short[] pcm = new short[0];

    /**
     * Captures stereo audio at 44.1 kHz from apps playing media, games or audio of unknown usage.
     */
    public PlaybackCapture(MediaProjection projection) {
        this(projection, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_COUNT,
            AudioAttributes.USAGE_MEDIA, AudioAttributes.USAGE_GAME, AudioAttributes.USAGE_UNKNOWN);
    }

    /**
     * @param channelCount 1 mono, 2 stereo
     * @param usages       the {@link AudioAttributes} usages to capture. Android only allows USAGE_MEDIA,
     *                     USAGE_GAME and USAGE_UNKNOWN.
     * @throws UnsupportedOperationException if the device can't record in this format
     */
    @SuppressLint("MissingPermission")
    public PlaybackCapture(MediaProjection projection, int sampleRate, int channelCount, int... usages) {
        if (channelCount != 1 && channelCount != 2) {
            throw new IllegalArgumentException("channelCount must be 1 or 2");
        }
        if (usages.length == 0) {
            throw new IllegalArgumentException("Pass at least one usage");
        }

        this.sampleRate = sampleRate;
        this.channelCount = channelCount;

        AudioPlaybackCaptureConfiguration.Builder config = new AudioPlaybackCaptureConfiguration.Builder(projection);

        for (int usage : usages) {
            config.addMatchingUsage(usage);
        }

        int channelMask = channelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;

        AudioFormat format = new AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(channelMask)
            .build();

        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT);

        record = new AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBufferSize * 4)
            .setAudioPlaybackCaptureConfig(config.build())
            .build();
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public int getChannelCount() {
        return channelCount;
    }

    @Override
    public void start() {
        record.startRecording();
    }

    @Override
    public int read(float[] buffer, int offset, int length) {
        length -= length % channelCount;

        if (pcm.length < length) {
            pcm = new short[length];
        }

        int count = record.read(pcm, 0, length, AudioRecord.READ_BLOCKING);
        if (count < 0) {
            return -1;
        }
        for (int i = 0; i < count; i++) {
            buffer[offset + i] = pcm[i] / 32768f;
        }

        return count;
    }

    @Override
    public void stop() {
        if (record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            record.stop();
        }
    }

    @Override
    public void close() {
        record.release();
    }
}
