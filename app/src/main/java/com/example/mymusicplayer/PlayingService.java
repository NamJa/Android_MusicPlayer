package com.example.mymusicplayer;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.RemoteViews;


import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

public class PlayingService extends Service implements MediaPlayer.OnPreparedListener{

    MediaPlayer mediaPlayer;

    NotificationCompat.Builder mBuilder;
    NotificationManager nm;
    public Intent intent1,intent2, nextIntent;
    public PendingIntent pintent1,pintent2, nextPendingIntent ;
    public RemoteViews contentView;
    String channelId = "channel";
    String channelName = "Channel Name";
    int importance = NotificationManager.IMPORTANCE_HIGH;
    int musicListSize = 0;
    Context context;
    public final int NOTIFICATION_ID = 1;

    public static boolean isPlayingMusic = false;
    public static final String MAIN_ACTION = "com.examplemymusicplayer.main";
    public static final String PLAY_ACTION = "com.examplemymusicplayer.play";
    public static final String PAUSE_ACTION = "com.examplemymusicplayer.pause";
    public static final String NEXTPLAY_ACTION = "com.examplemymusicplayer.nextplay";
    public static final String PREVPLAY_ACTION = "com.examplemymusicplayer.prevplay";
    public static final String STARTFOREGROUND_ACTION = "com.examplemymusicplayer.service.startforeground";
    public static final String STOPFOREGROUND_ACTION = "com.examplemymusicplayer.service.stopforegroud";
    //서비스 쪽에 사용되는 액션
    public static final String SERVICE_PLAY_ACTION = "com.examplemymusicplayer.playSERVICE";
    public static final String SERVICE_PAUSE_ACTION = "com.examplemymusicplayer.pauseSERVICE";

    int position = 0;
    int notiPosition = 0;
    ArrayList<MusicData> musicList;
    final IBinder playingServiceBind = new ServiceBinder();

    Intent playpauseAcitivyIntent;
    Intent nextActivityIntent;
    Intent prevActivityIntent;

    public class ServiceBinder extends Binder {
        PlayingService getService() {
            return PlayingService.this;
        }
    }
    @Override
    public void onCreate() {
        mediaPlayer = new MediaPlayer();

        super.onCreate();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        position = intent.getIntExtra("position", 0);
        musicList = (ArrayList<MusicData>) intent.getSerializableExtra("musicList");

        IntentFilter intentFilter = new IntentFilter(SERVICE_PLAY_ACTION);
        intentFilter.addAction(SERVICE_PAUSE_ACTION);
        registerReceiver(broadcastFromActitivy, intentFilter);
        //앨범 커버 눌렀을 때의 intent
        Intent playActivityIntent = new Intent(this, PlayMusicActivity.class);
        playActivityIntent.putExtra("musicName", musicList.get(position).getTitle());
        playActivityIntent.putExtra("position", position);
        playActivityIntent.putExtra("musicList", musicList);
        playActivityIntent.setAction(MAIN_ACTION);
        playActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent playActivitypendingIntent = PendingIntent.getActivity(this, 0, playActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // ////////////////////////////////////////////////////////////////////////////////////////////////// 이 부분의 기능은 합쳐야 됨
        //play버튼 눌렀을 때의 intent
        Intent playIntent = new Intent(this, PlayingService.class);
        playIntent.setAction(PLAY_ACTION);
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, 0);

        //pause버튼 눌렀을 때의 intent
        Intent pauseIntent = new Intent(this, PlayingService.class);
        pauseIntent.putExtra("position", position);
        pauseIntent.putExtra("musicList", musicList);
        pauseIntent.setAction(PAUSE_ACTION);
        PendingIntent pausePendingIntent = PendingIntent.getService(getApplicationContext(), 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // //////////////////////////////////////////////////////////////////////////////////////////////////// 이 부분의 기능은 합쳐야 됨


        ////broadcasting receiver intent
        Intent playpauseAcitivyIntent = new Intent();
        playpauseAcitivyIntent.setAction(PLAY_ACTION);
        Intent nextActivityIntent = new Intent();
        nextActivityIntent.putExtra("position", position);
        nextActivityIntent.putExtra("musicList", musicList);
        nextActivityIntent.setAction(NEXTPLAY_ACTION);
        Intent prevActivityIntent = new Intent();
        prevActivityIntent.putExtra("position", position);
        prevActivityIntent.putExtra("musicList", musicList);
        prevActivityIntent.setAction(PREVPLAY_ACTION);
        ////broadcasting receiver intent

        //다음 곡 재생 버튼을 눌렀을 때의 intent
        Intent nextIntent = new Intent(this, PlayingService.class);
        nextIntent.putExtra("position", getNextPosition(position));
        nextIntent.putExtra("musicList", musicList);
        nextIntent.setAction(NEXTPLAY_ACTION);
        PendingIntent nextPendingIntent = PendingIntent.getService(getApplicationContext(), 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //이전 곡 재생 버튼을 눌렀을 때의 intent
        Intent prevIntent = new Intent(this, PlayingService.class);
        prevIntent.putExtra("position", getPrevPosition(position));
        prevIntent.putExtra("musicList", musicList);
        prevIntent.setAction(PREVPLAY_ACTION);
        PendingIntent prevPendingIntent = PendingIntent.getService(getApplicationContext(), 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // x버튼 눌렀을 때의 intent (종료 버튼)
        Intent stopIntent = new Intent(this, PlayingService.class);
        stopIntent.setAction(STOPFOREGROUND_ACTION);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);


        mediaPlayer.setOnCompletionListener(mOnComplete);


        if(intent.getAction().equals(STARTFOREGROUND_ACTION)) {

            Intent recvIntent = new Intent(this, PlayMusicActivity.class);
            recvIntent.setAction(MAIN_ACTION);
            recvIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, recvIntent, 0);


        }  if(intent.getAction().equals(PLAY_ACTION)) {
            try {
                Uri musicURI = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + musicList.get(position).getId());
                mediaPlayer.setDataSource(getApplicationContext(), musicURI);
                mediaPlayer.prepare();
                mediaPlayer.start();
                isPlayingMusic = true;
                sendBroadcast(playpauseAcitivyIntent);
            }catch (Exception e) {
                e.printStackTrace();
            }

        }  if(intent.getAction().equals(PAUSE_ACTION)) {
            if(isPlayingMusic == true) {
                mediaPlayer.pause();
                isPlayingMusic = false;
                sendBroadcast(playpauseAcitivyIntent);
            } else if(isPlayingMusic == false) {
                mediaPlayer.start();
                isPlayingMusic = true;
                sendBroadcast(playpauseAcitivyIntent);
            }
            nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
                nm.createNotificationChannel(mChannel);
                mBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                        .setSmallIcon(mediaPlayer.isPlaying() ?android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause)
                        .setContent(contentView);
            } else {
                mBuilder = new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(mediaPlayer.isPlaying() ?android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause)
                        .setContent(contentView);
            }
            startForeground(1234, mBuilder.build());

        }  if(intent.getAction().equals(PREVPLAY_ACTION)) {
            try {
                context = getApplicationContext();
                position = intent.getIntExtra("position", 0);
                musicList = (ArrayList<MusicData>) intent.getSerializableExtra("musicList");
                Uri musicURI = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + musicList.get(position).getId());
                mediaPlayer.reset();
                mediaPlayer.setDataSource(getApplicationContext(), musicURI);
                mediaPlayer.prepare();
                mediaPlayer.start();
                sendBroadcast(prevActivityIntent);
            }catch (Exception e) {
                e.printStackTrace();
            }

        }  if(intent.getAction().equals(NEXTPLAY_ACTION)) {
            try {
                position = intent.getIntExtra("position", 0);
                musicList = (ArrayList<MusicData>) intent.getSerializableExtra("musicList");
                Uri musicURI = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + musicList.get(position).getId());
                mediaPlayer.reset();
                mediaPlayer.setDataSource(getApplicationContext(), musicURI);
                mediaPlayer.prepare();
                mediaPlayer.start();
                sendBroadcast(nextActivityIntent);
            }catch (Exception e) {
                e.printStackTrace();
            }

        }  if(intent.getAction().equals(STOPFOREGROUND_ACTION)) {
            stopForeground(true);
            stopSelf();
        }
        //알림창
        contentView = new RemoteViews(getPackageName(), R.layout.custom_notification);
        contentView.setTextViewText(R.id.notiMusicTitle, musicList.get(position).getTitle());
        contentView.setImageViewUri(R.id.notiAlbumArt, Uri.parse(musicList.get(position).getAlbumArtUri()));
        contentView.setImageViewResource(R.id.notiPauseButton, isPlayingMusic == true? android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play);
        contentView.setOnClickPendingIntent(R.id.notiAlbumArt, playActivitypendingIntent);
        contentView.setOnClickPendingIntent(R.id.notiPrevButton, prevPendingIntent);
        contentView.setOnClickPendingIntent(R.id.notiNextButton, nextPendingIntent);
        contentView.setOnClickPendingIntent(R.id.notiPauseButton, pausePendingIntent);
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
            nm.createNotificationChannel(mChannel);
            mBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                    .setSmallIcon(isPlayingMusic == false ?android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play)
                    .setOnlyAlertOnce(true)
                    .setContent(contentView);
        } else {
            mBuilder = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(isPlayingMusic == false ?android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play)
                    .setOnlyAlertOnce(true)
                    .setContent(contentView);
        }
        startForeground(1234, mBuilder.build());

        return START_STICKY;
    }

    BroadcastReceiver broadcastFromActitivy = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(SERVICE_PLAY_ACTION)) {
                nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
                    contentView.setImageViewResource(R.id.notiPauseButton, isPlayingMusic == true? android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play);
                    nm.createNotificationChannel(mChannel);
                    mBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                            .setSmallIcon(mediaPlayer.isPlaying() ?android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause)
                            .setOnlyAlertOnce(true)
                            .setContent(contentView);
                } else {
                    contentView.setImageViewResource(R.id.notiPauseButton, isPlayingMusic == true? android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play);
                    mBuilder = new NotificationCompat.Builder(getApplicationContext())
                            .setSmallIcon(mediaPlayer.isPlaying() ?android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause)
                            .setOnlyAlertOnce(true)
                            .setContent(contentView);
                }
                startForeground(1234, mBuilder.build());
            } else if(intent.getAction().equals(SERVICE_PAUSE_ACTION)) {
                nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
                    contentView.setImageViewResource(R.id.notiPauseButton, isPlayingMusic == true? android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play);
                    nm.createNotificationChannel(mChannel);
                    mBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                            .setSmallIcon(mediaPlayer.isPlaying() ?android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause)
                            .setOnlyAlertOnce(true)
                            .setContent(contentView);
                } else {
                    contentView.setImageViewResource(R.id.notiPauseButton, isPlayingMusic == true? android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play);
                    mBuilder = new NotificationCompat.Builder(getApplicationContext())
                            .setSmallIcon(mediaPlayer.isPlaying() ?android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause)
                            .setOnlyAlertOnce(true)
                            .setContent(contentView);
                }
                startForeground(1234, mBuilder.build());
            }
        }
    };

    @Override
    public void onDestroy() {
        mediaPlayer.release();
        super.onDestroy();
    }

    public MediaPlayer getMediaPlayer()
    {
        return mediaPlayer;
    }

    public int getNextPosition(int position)
    {
        int notiPosition = position;
        if(notiPosition >= (Integer)musicList.size()-1) {
            return notiPosition = 0;
        } else {
            return notiPosition += 1;
        }
    }
    public int getPrevPosition(int position)
    {
        int notiPosition = position;
        if(notiPosition == 0) {
            return (notiPosition = (Integer)musicList.size()-1);
        } else {
            return notiPosition -= 1;
        }
    }

    MediaPlayer.OnCompletionListener mOnComplete =
    new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer arg0) {
            position = getNextPosition(position);
            try {
                mediaPlayer.reset();
                Uri musicURI = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + musicList.get(position).getId());
                mediaPlayer.reset();
                mediaPlayer.setDataSource(getApplicationContext(), musicURI);
                mediaPlayer.prepare();
                mediaPlayer.start();
                Intent nextMusicIntent = new Intent();
                nextMusicIntent.setAction(NEXTPLAY_ACTION);
                nextMusicIntent.putExtra("position", position);
                nextMusicIntent.putExtra("musicList", musicList);
                sendBroadcast(nextMusicIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            contentView.setTextViewText(R.id.notiMusicTitle, musicList.get(position).getTitle());
            contentView.setImageViewUri(R.id.notiAlbumArt, Uri.parse(musicList.get(position).getAlbumArtUri()));
            nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
                nm.createNotificationChannel(mChannel);
                mBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                        .setSmallIcon(isPlayingMusic == false ?android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play)
                        .setOnlyAlertOnce(true)
                        .setContent(contentView);
            } else {
                mBuilder = new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(isPlayingMusic == false ?android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play)
                        .setOnlyAlertOnce(true)
                        .setContent(contentView);
            }
            startForeground(1234, mBuilder.build());
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return playingServiceBind;
    }
}
