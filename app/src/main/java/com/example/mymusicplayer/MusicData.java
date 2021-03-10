package com.example.mymusicplayer;

import android.net.Uri;
import android.os.Parcelable;

import java.io.Serializable;

public class MusicData implements Serializable {
    private String id;
    private long albumId;
    private String artUri;
    private String title;
    private String artist;
    private String durTime;


    public MusicData() {

    }

    public MusicData(String id, long albumId, String title, String artist){
        this.id = id;
        this.albumId = albumId;
        this.title = title;
        this.artist = artist;
    }
    public String getId() {return id;}
    public long getAlbumId() {return albumId;}
    public String getTitle() {return title;}
    public String getArtist() {return artist;}
    public String getDurTime() {return durTime;}
    public String getAlbumArtUri() {return artUri;}

    public void setId(String id)
    {
        this.id = id;
    }
    public void setAlbumId(long albumId)
    {
        this.albumId = albumId;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }
    public void setArtist(String artist)
    {
        this.artist = artist;
    }
    public void setDurTime(String durTime) {this.durTime = durTime;}
    public void setAlbumArtUri(Uri artUri)
    {
        this.artUri = artUri.toString();
    }
}
