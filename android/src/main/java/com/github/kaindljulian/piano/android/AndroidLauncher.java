package com.github.kaindljulian.piano.android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.github.kaindljulian.piano.Main;
import com.github.kaindljulian.piano.android.audio.PlaybackCapture;
import com.github.kaindljulian.piano.android.audio.PlaybackCaptureRequest;
import com.github.kaindljulian.piano.audio.AudioCaptureReader;
import com.github.kaindljulian.piano.audio.MusicAnalyzer;

import java.util.function.Consumer;

/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {
    private static final int RECORD_AUDIO_REQUEST = 1;

    private final MusicAnalyzer music =
        new MusicAnalyzer(PlaybackCapture.DEFAULT_SAMPLE_RATE, PlaybackCapture.DEFAULT_CHANNEL_COUNT);
    private MediaProjection projection;
    private AudioCaptureReader reader;
    private Consumer<Boolean> pending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true;
        initialize(new Main(music, this::selectAudioSource), configuration);
    }

    private void selectAudioSource(Consumer<Boolean> done) {
        runOnUiThread(() -> {
            stopCapture();
            music.reset();
            pending = done;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST);
        });
    }

    private void answer(boolean allowed) {
        Consumer<Boolean> done = pending;
        pending = null;
        if (done != null) {
            done.accept(allowed);
        }
    }

    private void stopCapture() {
        if (reader != null) {
            reader.close();
            reader = null;
        }
        if (projection != null) {
            projection.stop();
            projection = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != RECORD_AUDIO_REQUEST) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestCapture();
        } else {
            answer(false);
        }
    }

    private void requestCapture() {
        PlaybackCaptureRequest.request(this, new PlaybackCaptureRequest.Callback() {
            @Override
            public void onGranted(MediaProjection granted) {
                projection = granted;
                reader = new AudioCaptureReader(new PlaybackCapture(granted), 2048, music);
                answer(true);
            }

            @Override
            public void onDenied() {
                answer(false);
            }
        });
    }

    @Override
    protected void onDestroy() {
        stopCapture();
        super.onDestroy();
    }
}
