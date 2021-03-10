package com.example.mymusicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.example.mymusicplayer.PlayingService.ServiceBinder;

import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class PlayMusicActivity extends AppCompatActivity {

    NotificationCompat.Builder mBuilder;
    NotificationManager nm;
    public Intent intent1,intent2, nextIntent;
    public PendingIntent pintent1,pintent2, nextPendingIntent ;
    public RemoteViews contentView;
    String channelId = "channel";
    String channelName = "Channel Name";
    int importance = NotificationManager.IMPORTANCE_HIGH;
    int notiPostion = 0;

    public final int NOTIFICATION_ID = 1;
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

    PlayingService playingService;
    MediaPlayer mediaPlayer;
    boolean isService = false;
    ImageView albumArt, prevButton, playButton, pauseButton, nextButton;
    TextView musicTitleTv, currentPlayingTimeTv, musicRunningTimeTv;
    SeekBar seekBar;

    int position;
    String musicTitle;
    ArrayList<MusicData> musicList;
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("life", "서비스 연결됨");
            ServiceBinder sb = (ServiceBinder) service;
            playingService = sb.getService();
            isService = true;
            getMediaPlayerInfo();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("life", "서비스 연결 해제");
            isService = false;
        }
    };


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(PLAY_ACTION)) {
                if(PlayingService.isPlayingMusic == true) {
                    playButton.setVisibility(View.GONE);
                    pauseButton.setVisibility(View.VISIBLE);
                } else if (PlayingService.isPlayingMusic == false) {
                    playButton.setVisibility(View.VISIBLE);
                    pauseButton.setVisibility(View.GONE);
                }
            } else if(intent.getAction().equals(NEXTPLAY_ACTION)) {
                seekBar.setMax(mediaPlayer.getDuration());
                int position = intent.getIntExtra("position", 0);
                ArrayList<MusicData> musicList = (ArrayList<MusicData>) intent.getSerializableExtra("musicList");
                musicTitleTv.setText(musicList.get(position).getTitle());
                albumArt.setImageURI(Uri.parse(musicList.get(position).getAlbumArtUri()));
            } else if (intent.getAction().equals(PREVPLAY_ACTION)) {
                seekBar.setMax(mediaPlayer.getDuration());
                int position = intent.getIntExtra("position", 0);
                ArrayList<MusicData> musicList = (ArrayList<MusicData>) intent.getSerializableExtra("musicList");
                musicTitleTv.setText(musicList.get(position).getTitle());
                albumArt.setImageURI(Uri.parse(musicList.get(position).getAlbumArtUri()));
            }
        }
    };


    public void getMediaPlayerInfo()
    {
        mediaPlayer = playingService.getMediaPlayer();
        seekBar.setProgress(0);
        seekBar.setMax(mediaPlayer.getDuration());
        if(mediaPlayer.isPlaying())
        {
            playButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            seekBarHandler.sendEmptyMessageDelayed(0, 200);
        }
    }

    public void setPlayer()
    {
        musicTitleTv.setText("" + musicList.get(position).getTitle());
        albumArt.setImageURI(Uri.parse(musicList.get(position).getAlbumArtUri()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_music);

        albumArt = findViewById(R.id.albumArt);
        prevButton = findViewById(R.id.prevImg);
        nextButton = findViewById(R.id.nextImg);
        playButton = findViewById(R.id.playImg);
        pauseButton = findViewById(R.id.pauseImg);
        musicTitleTv = findViewById(R.id.musicTitlePlayer);
        currentPlayingTimeTv = findViewById(R.id.currentPlayTime);
        musicRunningTimeTv = findViewById(R.id.musicRunningTime);
        seekBar = findViewById(R.id.musicSeekBar);

        IntentFilter intentFilter = new IntentFilter(PLAY_ACTION);
        intentFilter.addAction(NEXTPLAY_ACTION);
        intentFilter.addAction(PREVPLAY_ACTION);
        IntentFilter intentFilter1 = new IntentFilter(NEXTPLAY_ACTION);
        registerReceiver(broadcastReceiver, intentFilter);

        //본격적인 intent 수신 구현부분
        Intent intentFromList = getIntent();
        if(intentFromList != null) {
            musicTitle = intentFromList.getStringExtra("musicName");
            position = intentFromList.getIntExtra("position", 0);
            musicList = (ArrayList<MusicData>) intentFromList.getSerializableExtra("musicList");

            musicTitleTv.setText("" + musicTitle);
            albumArt.setImageURI(Uri.parse(musicList.get(position).getAlbumArtUri()));

            Intent sendServiceIntent = new Intent(getApplicationContext(), PlayingService.class);
            sendServiceIntent.setAction(PLAY_ACTION);
            sendServiceIntent.putExtra("musicName", musicTitle  );
            sendServiceIntent.putExtra("position", position);
            sendServiceIntent.putExtra("musicList", musicList);
            startService(sendServiceIntent);
            bindService(sendServiceIntent, conn, BIND_AUTO_CREATE);
            if(PlayingService.isPlayingMusic == true) {

                playButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);

            }

        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int m = progress / 60000;
                int s = (progress % 60000) / 1000;
                int totM = mediaPlayer.getDuration() / 60000;
                int totS = (mediaPlayer.getDuration() % 60000) / 1000;
                if (s < 10) {
                    currentPlayingTimeTv.setText(m + ":0" + s);
                } else {
                    currentPlayingTimeTv.setText(m + ":" + s);
                }
                if (totS < 10)
                    musicRunningTimeTv.setText(totM + ":0" + totS);
                else {
                    musicRunningTimeTv.setText(totM + ":" + totS);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mediaPlayer.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo(seekBar.getProgress());
                if(PlayingService.isPlayingMusic == true)
                    mediaPlayer.start();
                else if(PlayingService.isPlayingMusic == false) {
                }
            }
        });
        seekBarHandler.sendEmptyMessageDelayed(0, 200);


        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(position == 0) {
                    position = musicList.size() - 1;
                } else {
                    position -= 1;
                }
                Intent sendServiceIntent = new Intent(getApplicationContext(), PlayingService.class);
                sendServiceIntent.setAction(PREVPLAY_ACTION);
                sendServiceIntent.putExtra("musicName", musicTitle  );
                sendServiceIntent.putExtra("position", position);
                sendServiceIntent.putExtra("musicList", musicList);
                startService(sendServiceIntent);
                setPlayer();
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(position >= musicList.size()-1) {
                    position = 0;
                } else {
                    position += 1;
                }
                Intent sendServiceIntent = new Intent(getApplicationContext(), PlayingService.class);
                sendServiceIntent.setAction(NEXTPLAY_ACTION);
                sendServiceIntent.putExtra("musicName", musicTitle  );
                sendServiceIntent.putExtra("position", position);
                sendServiceIntent.putExtra("musicList", musicList);
                startService(sendServiceIntent);
                setPlayer();
            }
        });
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
                Intent sendServiceFilter = new Intent();
                sendServiceFilter.setAction(SERVICE_PLAY_ACTION);
                mediaPlayer = playingService.getMediaPlayer();
                PlayingService.isPlayingMusic = true;
                mediaPlayer.start();
                sendBroadcast(sendServiceFilter);
            }
        });
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseButton.setVisibility(View.GONE);
                playButton.setVisibility(View.VISIBLE);
                Intent sendServiceFilter = new Intent();
                sendServiceFilter.setAction(SERVICE_PAUSE_ACTION);
                mediaPlayer = playingService.getMediaPlayer();
                PlayingService.isPlayingMusic = false;
                mediaPlayer.pause();
                sendBroadcast(sendServiceFilter);
            }
        });


    }
    Handler seekBarHandler = new Handler(){
        public void handleMessage(Message msg) {
           // mediaPlayer = playingService.getMediaPlayer();
            if(mediaPlayer == null)
                return;
            try {
                if (mediaPlayer.isPlaying() && mediaPlayer != null) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            seekBarHandler.sendEmptyMessageDelayed(0, 200);
        }
    };

    @Override
    protected void onStart() {
        Log.d("life", "onStart");
        if(PlayingService.isPlayingMusic == true) {
            playButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
        } else if (playingService.isPlayingMusic == false) {
            playButton.setVisibility(View.VISIBLE);
            pauseButton.setVisibility(View.GONE);
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d("life", "onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {

        unbindService(conn);
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}