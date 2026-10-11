package com.github.kaindljulian.piano.android.audio;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidEventListener;

/**
 * Asks the user to allow capturing audio from other apps and hands back a {@link MediaProjection}
 * for a {@link PlaybackCapture}. Android shows its screen sharing consent dialog for this.
 * This class doesn't request the RECORD_AUDIO permission, which the capture also needs.
 */
@TargetApi(Build.VERSION_CODES.Q)
public final class PlaybackCaptureRequest {
    public interface Callback {
        /**
         * Called on the main thread. Call {@link MediaProjection#stop()} when you're done capturing,
         * which also stops the foreground service and its notification.
         */
        void onGranted(MediaProjection projection);

        /**
         * Called on the main thread when the user declines.
         */
        void onDenied();
    }

    private static final int REQUEST_CODE = 0x7064;

    private PlaybackCaptureRequest() {
    }

    /**
     * Shows the consent dialog from a libGDX activity and handles its result.
     */
    public static void request(AndroidApplication application, Callback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        application.addAndroidEventListener(new AndroidEventListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                if (requestCode != REQUEST_CODE) {
                    return;
                }
                // libGDX iterates the listeners by index, so removing one during dispatch would skip the next.
                mainHandler.post(() -> application.removeAndroidEventListener(this));
                handleResult(application, resultCode, data, callback);
            }
        });
        application.startActivityForResult(createIntent(application), REQUEST_CODE);
    }

    /**
     * The consent dialog intent, for activities that aren't a libGDX {@link AndroidApplication}.
     */
    public static Intent createIntent(Context context) {
        return context.getSystemService(MediaProjectionManager.class).createScreenCaptureIntent();
    }

    /**
     * Pass the result of the {@link #createIntent} activity here.
     */
    public static void handleResult(Context context, int resultCode, Intent data, Callback callback) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            callback.onDenied();
            return;
        }

        PlaybackCaptureService.pending = callback;
        Intent service = new Intent(context, PlaybackCaptureService.class)
            .putExtra(PlaybackCaptureService.EXTRA_RESULT_CODE, resultCode)
            .putExtra(PlaybackCaptureService.EXTRA_RESULT_DATA, data);

        context.startForegroundService(service);
    }
}
