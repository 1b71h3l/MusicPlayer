package com.example.musicplayer;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import java.util.Objects;

public class PlayerService extends Service {

    private final IBinder serviceBinder = new ServiceBinder();

    //player
    ExoPlayer player;
    PlayerNotificationManager notificationManager;


    public class ServiceBinder extends Binder{
        public PlayerService getPlayerService(){
            return PlayerService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
       return serviceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(getApplicationContext()).build();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player.setAudioAttributes(audioAttributes,true);

        final String channelId = getResources().getString(R.string.app_name) + "Music Channel";
        final int notificationId = 11111111;
        notificationManager= new PlayerNotificationManager.Builder(this,notificationId,channelId)
                .setNotificationListener(notificationListener)
                .setMediaDescriptionAdapter(descriptionadapter)
                .setChannelImportance(IMPORTANCE_HIGH)
                .setSmallIconResourceId(R.drawable.small)
                .setNextActionIconResourceId(R.drawable.baseline_arrow_forward)
                .setPreviousActionIconResourceId(R.drawable.baseline_arrow_back)
                .setPauseActionIconResourceId(R.drawable.baseline_pause)
                .setPlayActionIconResourceId(R.drawable.baseline_play_arrow)
                .setChannelNameResourceId(R.string.app_name)
                .build();
        notificationManager.setPlayer(player);
        notificationManager.setPriority(NotificationCompat.PRIORITY_MAX);
        notificationManager.setUseRewindAction(false);
        notificationManager.setUseFastForwardAction(false);

    }



    PlayerNotificationManager.NotificationListener notificationListener = new PlayerNotificationManager.NotificationListener() {
        @Override
        public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
            PlayerNotificationManager.NotificationListener.super.onNotificationCancelled(notificationId, dismissedByUser);
            stopForeground(true);
            if(player.isPlaying()){
                player.pause();
            }
            stopSelf();
        }

        @Override
        public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
            PlayerNotificationManager.NotificationListener.super.onNotificationPosted(notificationId, notification, ongoing);
            startForeground(notificationId,notification);
        }
    };

    PlayerNotificationManager.MediaDescriptionAdapter descriptionadapter = new PlayerNotificationManager.MediaDescriptionAdapter() {
        @Override
        public CharSequence getCurrentContentTitle(Player player) {
            return Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title;
        }

        @Nullable
        @Override
        public PendingIntent createCurrentContentIntent(Player player) {
           //intent to open the app when the notification is clicked
            Intent openAppIntent = new Intent(getApplicationContext(),MainActivity.class);
            return PendingIntent.getActivity(getApplicationContext(),0,openAppIntent,PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }

        @Nullable
        @Override
        public CharSequence getCurrentContentText(Player player) {
            return null;
        }

        @Nullable
        @Override
        public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
            ImageView view = new ImageView(getApplicationContext());
            view.setImageURI(player.getCurrentMediaItem().mediaMetadata.artworkUri);
            BitmapDrawable bitmapDrawable = (BitmapDrawable) view.getDrawable();

            if (bitmapDrawable == null) {
                bitmapDrawable= (BitmapDrawable) ContextCompat.getDrawable(getApplicationContext(),R.drawable.music_icon);
            }
            assert bitmapDrawable != null;
            return bitmapDrawable.getBitmap();
        }
    };

    @Override
    public void onDestroy() {
        if(player.isPlaying()) player.stop();
        notificationManager.setPlayer(null);
        player.release();
        player=null;
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }
}