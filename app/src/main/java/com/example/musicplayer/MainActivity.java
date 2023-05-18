package com.example.musicplayer;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.Player;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Database database;
    List<Song> allSongs = new ArrayList<>();
    ActivityResultLauncher<String> storagePermissionLauncher;
    final String permission = android.Manifest.permission.READ_EXTERNAL_STORAGE;
    ExoPlayer player;
    private TextView title;
    private ImageView playButton, pauseButton, backButton, nextButton, favButton, favaButton , image;
    boolean isBound = false;
    Intent playerServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //*****the permission
        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (!granted) {
                userResponse();
            }
        });

        //Initialisations
        title = findViewById(R.id.songName);
        playButton = findViewById(R.id.btnPlay);
        pauseButton = findViewById(R.id.btnPause);
        backButton = findViewById(R.id.btnBack);
        nextButton = findViewById(R.id.btnNext);
        favButton = findViewById(R.id.favorite_button);
        favaButton = findViewById(R.id.favoritea_button);
        image = findViewById(R.id.gifimage);
        allSongs.clear();
        allSongs.addAll(fetchSongs());

        //bind to the player service , and do everything after the binding
        doBindService();

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!player.isPlaying()) {
                    //startService(new Intent(getApplicationContext(),PlayerService.class));
                    pauseButton.setVisibility(View.VISIBLE);
                    backButton.setVisibility(View.VISIBLE);
                    nextButton.setVisibility(View.VISIBLE);
                    updateFavoriteButtons();
                    title.setVisibility(View.VISIBLE);
                    // Hide the play button
                    playButton.setVisibility(View.GONE);
                    player.prepare();
                    player.play();
                }
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (player.isPlaying()) {
                    pauseButton.setVisibility(View.GONE);
                    backButton.setVisibility(View.GONE);
                    nextButton.setVisibility(View.GONE);
                    favButton.setVisibility(View.GONE);
                    favaButton.setVisibility(View.GONE);
                    title.setVisibility(View.GONE);
                    // Hide the play button
                    playButton.setVisibility(View.VISIBLE);
                    player.pause();
                   // player.seekTo(0,0);  //restart from the begining
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(player.hasNextMediaItem()){
                    player.seekToNext();
                }else{
                    player.seekTo(0,0);
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(player.hasPreviousMediaItem()){
                    player.seekToPrevious();
                }
            }
        });

        //favorites
        database = new Database(this);
        favButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToFavorites();
            }
        });

        favaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteFromFavorites();
            }
        });
    }



    private void doBindService() {
        playerServiceIntent = new Intent(this, PlayerService.class);
        bindService(playerServiceIntent,playerServiceConnection, Context.BIND_AUTO_CREATE);
        isBound=true;
    }

    ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            PlayerService.ServiceBinder binder = (PlayerService.ServiceBinder) iBinder;
            player =binder.getPlayerService().player;
            isBound=true;
            storagePermissionLauncher.launch(permission);
            playerControls();
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    private void playerControls() {
        //Song song= allSongs.get(0);
        player.setMediaItems(getMediaItems(), 0, 0);
        title.setText(player.getCurrentMediaItem().mediaMetadata.title);

        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                assert mediaItem != null;
                title.setText(mediaItem.mediaMetadata.title);
                Uri artworkUri = mediaItem.mediaMetadata.artworkUri;
                if (artworkUri != null) {
                    image.setImageURI(artworkUri);
                    if (image.getDrawable() == null) {
                        image.setImageResource(R.drawable.music);
                    }
                }
                updateFavoriteButtons();
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                 if(playbackState == ExoPlayer.STATE_READY){
                     title.setText(player.getCurrentMediaItem().mediaMetadata.title);
                     Uri artworkUri = player.getCurrentMediaItem().mediaMetadata.artworkUri;
                     if(artworkUri!= null){
                         image.setImageURI(artworkUri);
                         if(image.getDrawable()==null) {
                             image.setImageResource(R.drawable.music);
                         }}
                     updateFavoriteButtons();
                 }
            }
        });
    }


    private List<MediaItem> getMediaItems() {
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : allSongs){
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(song.getUri())
                    .setMediaMetadata(getMetadata(song))
                    .build();
            mediaItems.add(mediaItem);
        }
        return mediaItems;
    }

    private MediaMetadata getMetadata(Song song) {
        return new MediaMetadata.Builder()
                .setTitle(song.getTitle())
                .setArtworkUri(song.getArtworkUri())
                .build();
    }


    private void userResponse() {
        if(ContextCompat.checkSelfPermission(this,permission)== PackageManager.PERMISSION_GRANTED){
            Toast.makeText(MainActivity.this, "permission available", Toast.LENGTH_SHORT).show();
        }else if ( Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            if(shouldShowRequestPermissionRationale(permission)){
                new AlertDialog.Builder(this)
                        .setTitle("Requesting Permission")
                        .setMessage("Allow fetching songs on your device")
                        .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                storagePermissionLauncher.launch(permission);

                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(MainActivity.this, "You didn't allow the application to fetch for songs ", Toast.LENGTH_SHORT).show();
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            }

        }
        else {
            Toast.makeText(MainActivity.this, "You canceled showing the songs", Toast.LENGTH_SHORT).show();
        }
    }

    private List<Song> fetchSongs() {
       // Toast.makeText(MainActivity.this, "Fetching songs...", Toast.LENGTH_LONG).show();
        List<Song> songs = new ArrayList<>();
        Uri mediaStoreUri;

        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q){
            mediaStoreUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }else{
            mediaStoreUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ALBUM_ID,
        };
        String sortOrder= MediaStore.Audio.Media.DATE_ADDED + " DESC";
        try (Cursor cursor = getContentResolver().query(mediaStoreUri , projection,null,null,sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn= cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int nameColumn= cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                int albumIdColumn= cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

                do {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    long albumId = cursor.getLong(albumIdColumn);

                    Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id);

                    Uri albumUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),albumId);

                    //remove the extension .mp3 from the name of the song
                    name = name.substring(0,name.lastIndexOf("."));

                    Song song = new Song (name ,uri , albumUri);

                    //add song to the list
                    songs.add(song);
                } while (cursor.moveToNext());
            }
            //Toast.makeText(MainActivity.this, "Songs fetched successfully", Toast.LENGTH_LONG).show();
            return songs;
        }
    }

    //The menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_download) {
            Intent intent = new Intent(this, DownloadActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_favorites) {
            Intent intent = new Intent(this, FavoritesActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //the favorites part

    private void updateFavoriteButtons() {
        if (isSongInFavorites()) {
            favButton.setVisibility(View.GONE);
            favaButton.setVisibility(View.VISIBLE);
        } else {
            favButton.setVisibility(View.VISIBLE);
            favaButton.setVisibility(View.GONE);
        }

    }

    private boolean isSongInFavorites() {
        List<Song> favoritesList = database.getAllSongs();
        for (Song song : favoritesList) {
            if (song.getTitle().equals(player.getCurrentMediaItem().mediaMetadata.title)) {
                return true;
            }
        }
        return false;
    }

    private void deleteFromFavorites() {
        database.deleteFavorite((String) player.getCurrentMediaItem().mediaMetadata.title);
        Toast.makeText(this, "Song deleted from favorites", Toast.LENGTH_SHORT).show();
        updateFavoriteButtons();
    }

    private void addToFavorites() {
        long id = database.createFavorite((String) player.getCurrentMediaItem().mediaMetadata.title);
        if (id != -1) {
            Toast.makeText(this, "Song added to favorites", Toast.LENGTH_SHORT).show();
            updateFavoriteButtons();
        } else {
            Toast.makeText(this, "Failed to add song to favorites", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
        stopService(playerServiceIntent);
    }

    private void doUnbindService() {
        if(isBound){
            unbindService(playerServiceConnection);
            isBound=false;
        }
    }
}