package com.github.kaindljulian.piano.android.audio;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.github.kaindljulian.piano.R;

import java.util.Objects;

/**
 * The foreground service Android requires while an app holds the {@link MediaProjection} isntance.
 * {@link PlaybackCaptureRequest} starts it, and it stops itself when the projection stops.
 */
@TargetApi(Build.VERSION_CODES.Q)
public class PlaybackCaptureService extends Service {
    static final String EXTRA_RESULT_CODE = "resultCode";
    static final String EXTRA_RESULT_DATA = "resultData";
    private static final String CHANNEL_ID = "playback_capture";
    private static final int NOTIFICATION_ID = 1;

    /**
     * The callback waiting for the next projection. Only touched on the main thread.
     */
    static PlaybackCaptureRequest.Callback pending;

    private MediaProjection projection;

    @Override
    @SuppressWarnings("deprecation")
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);

        Intent data = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class)
            : intent.getParcelableExtra(EXTRA_RESULT_DATA);

        MediaProjection previous = projection;
        MediaProjection next = getSystemService(MediaProjectionManager.class).getMediaProjection(resultCode, data);

        projection = next;

        Objects.requireNonNull(next).registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                if (projection == next) stopSelf();
            }
        }, new Handler(Looper.getMainLooper()));

        if (previous != null) {
            previous.stop();
        }

        PlaybackCaptureRequest.Callback callback = pending;
        pending = null;

        if (callback != null) {
            callback.onGranted(next);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        MediaProjection stopping = projection;
        projection = null;
        if (stopping != null) {
            stopping.stop();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createNotification() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.capture_channel_name), NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        return new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(getApplicationInfo().icon)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setOngoing(true)
            .build();
    }
}
