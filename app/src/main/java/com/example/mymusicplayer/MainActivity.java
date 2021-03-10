package com.example.mymusicplayer;

import com.example.mymusicplayer.PlayingService.ServiceBinder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static String MAIN_ACTION = "com.examplemymusicplayer.main";
    public static String PLAY_ACTION = "com.examplemymusicplayer.play";
    public static String PAUSE_ACTION = "com.examplemymusicplayer.pause";
    public static String NEXTPLAY_ACTION = "com.examplemymusicplayer.nextplay";
    public static String PREVPLAY_ACTION = "com.examplemymusicplayer.prevplay";
    public static String STARTFOREGROUND_ACTION = "com.examplemymusicplayer.service.startforeground";
    public static String STOPFOREGROUND_ACTION = "com.examplemymusicplayer.service.stopforegroud";

    PlayingService playingService;
    boolean isService = false;
    MediaPlayer mediaPlayer;

    ListView listView = null;
    BaseAdapterEx adapter = null;
    ArrayList<String> data = null;
    public ArrayList<MusicData> musicList;

    private static final int PERMISSION_REQUEST = 1;

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("life", "MainActivity onServiceConnect 연결");
            ServiceBinder sb = (ServiceBinder) service;
            playingService = sb.getService();
            isService = true;
            mediaPlayer = playingService.getMediaPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isService = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.FOREGROUND_SERVICE, }, PERMISSION_REQUEST);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.FOREGROUND_SERVICE}, PERMISSION_REQUEST);
            }
        } else {
            try {
                doListing();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }



        adapter = new BaseAdapterEx(this, musicList);

        listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(isMyServiceRunning(PlayingService.class) == true)
                {
                    Intent isServiceRunIntent = new Intent(getApplicationContext(), PlayingService.class);
                    isServiceRunIntent.setAction(STOPFOREGROUND_ACTION);
                    startService(isServiceRunIntent);
                }

                Toast.makeText(getApplicationContext(), musicList.get(position).getTitle(), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getApplicationContext(), PlayMusicActivity.class);

                intent.putExtra("musicName", musicList.get(position).getTitle());
                intent.putExtra("position", position);
                intent.putExtra("musicList", musicList);
                startActivity(intent);
            }
        });
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void doListing()
    {
        listView = (ListView)findViewById(R.id.list_view);
        getMusicList();
    }

    public Uri getAlbumUri(Context mContext, String album_id) {
        if (mContext != null) {
            Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            Uri imageUri = Uri.withAppendedPath(sArtworkUri, String.valueOf(album_id));
            return imageUri;
        }
        return null;
    }

    public void getMusicList() {
        musicList = new ArrayList<>();
        String[] musicProjection = {MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST};
        Uri albumArtMass = Uri.parse("content://media/external/audio/albumart");

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicProjection, null, null, null);

        while (cursor.moveToNext()) {
            MusicData musicData = new MusicData();
            musicData.setId(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
            musicData.setAlbumId(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
            musicData.setAlbumArtUri(ContentUris.withAppendedId(albumArtMass, musicData.getAlbumId()));
            musicData.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
            musicData.setArtist(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            musicList.add(musicData);

        }
        cursor.close();
    }

    @Override
    protected void onDestroy() {
        //unbindService(conn);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case PERMISSION_REQUEST: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission granted" , Toast.LENGTH_LONG).show();

                        doListing();
                    }
                } else {
                    Toast.makeText(this, "No Permission granted", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
        }
    }
}