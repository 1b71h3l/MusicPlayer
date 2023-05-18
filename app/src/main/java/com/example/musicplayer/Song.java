package com.example.musicplayer;

import android.net.Uri;

import java.util.List;

public class Song {
    String title;
    Uri uri ;
    Uri artworkUri;


    public Song(String title, Uri uri, Uri artworkUri) {
        this.title = title;
        this.uri = uri;
        this.artworkUri = artworkUri;
    }

    public Song() {
    }

    public String getTitle() {
        return title;
    }

    public Uri getUri() {
        return uri;
    }

    public Uri getArtworkUri() {
        return artworkUri;
    }


    public void setTitle(String title) {
        this.title = title;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public void setArtworkUri(Uri artworkUri) {
        this.artworkUri = artworkUri;
    }


}
